/*
 *  This file is part of the tock-corenlp distribution.
 *  (https://github.com/theopenconversationkit/tock-corenlp)
 *  Copyright (c) 2017 VSCT.
 *
 *  tock-corenlp is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, version 3.
 *
 *  tock-corenlp is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package ai.tock.nlp.stanford

import ai.tock.nlp.core.configuration.NlpApplicationConfiguration
import ai.tock.nlp.core.configuration.NlpModelConfiguration
import ai.tock.nlp.core.sample.SampleEntity
import ai.tock.nlp.core.sample.SampleExpression
import ai.tock.nlp.model.EntityBuildContext
import ai.tock.nlp.model.IntentContext
import ai.tock.nlp.model.TokenizerContext
import ai.tock.nlp.model.service.engine.EntityModelHolder
import ai.tock.nlp.model.service.engine.IntentModelHolder
import ai.tock.nlp.model.service.engine.NlpEngineModelBuilder
import ai.tock.nlp.model.service.engine.TokenizerModelHolder
import ai.tock.shared.loadProperties
import edu.stanford.nlp.classify.ColumnDataClassifier
import edu.stanford.nlp.classify.Dataset
import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.objectbank.ObjectBank
import mu.KotlinLogging


/**
 *
 */
internal object StanfordModelBuilder : NlpEngineModelBuilder {

    private val logger = KotlinLogging.logger {}

    const val TAB = "\t"
    const val ADJACENT_ENTITY_MARKER = "__near__"

    override fun buildTokenizerModel(
        context: TokenizerContext,
        configuration: NlpApplicationConfiguration,
        expressions: List<SampleExpression>
    ): TokenizerModelHolder {
        return TokenizerModelHolder(context.language, configuration)
    }

    override val defaultEntityClassifierConfiguration: NlpModelConfiguration =
        NlpModelConfiguration(loadProperties("/stanford/crfclassifier.properties"))

    override val defaultIntentClassifierConfiguration: NlpModelConfiguration =
        NlpModelConfiguration(loadProperties("/stanford/intentClassifier.properties"))

    override val defaultTokenizerConfiguration: NlpModelConfiguration =
        NlpModelConfiguration(loadProperties("/stanford/tokenizer.properties"))

    override fun buildIntentModel(
        context: IntentContext,
        configuration: NlpApplicationConfiguration,
        expressions: List<SampleExpression>
    ): IntentModelHolder {
        val cdc = ColumnDataClassifier(configuration.intentConfiguration.properties)
        val dataset = Dataset<String, String>()
        expressions.map {
            dataset.add(cdc.makeDatumFromLine("${it.intent.name}$TAB$${it.text}"))
        }
        val classifier = cdc.makeClassifier(dataset)
        return IntentModelHolder(context.application, StanfordIntentModel(cdc, classifier), configuration)
    }

    override fun buildEntityModel(
        context: EntityBuildContext,
        configuration: NlpApplicationConfiguration,
        expressions: List<SampleExpression>
    ): EntityModelHolder {
        val crfClassifier = CRFClassifier<CoreLabel>(configuration.entityConfiguration.properties)
        val trainingData = getEntityTrainData(context, configuration, expressions)
        try {
            val transformedData: ObjectBank<MutableList<CoreLabel>> = crfClassifier.makeObjectBankFromString(
                trainingData,
                crfClassifier.defaultReaderAndWriter()
            )
            crfClassifier.train(transformedData)
            return EntityModelHolder(crfClassifier, configuration)
        } catch (e: Exception) {
            logger.error {
                "error with train data: \n $trainingData"
            }
            throw e
        }
    }

    internal fun getEntityTrainData(
        context: EntityBuildContext,
        configuration: NlpApplicationConfiguration,
        expressions: List<SampleExpression>
    ): String {
        val tokenizer =
            StanfordEngineProvider.getStanfordTokenizer(TokenizerModelHolder(context.language, configuration))
        val tokenizerContext = TokenizerContext(context)
        val sb = StringBuilder()
        val tokensIndexes: MutableMap<Int, SampleEntity> = HashMap()
        val entityRoleMap: MutableMap<SampleEntity, String> = HashMap()
        expressions.forEach exp@{ expression ->
            try {
                val text = expression.text
                if (text.contains("\n") || text.contains("\t")) {
                    logger.warn { "expression $text contains \\n or \\t!!! - skipped" }
                    return@exp
                }
                tokensIndexes.clear()
                entityRoleMap.clear()
                val tokens = tokenizer.tokenize(tokenizerContext, text)
                expression.entities.forEach { e ->
                    val start =
                        if (e.start == 0) 0 else tokenizer.tokenize(tokenizerContext, text.substring(0, e.start)).size
                    val end = start + tokenizer.tokenize(tokenizerContext, text.substring(e.start, e.end)).size
                    if (start < tokens.size && end <= tokens.size) {
                        for (i in start until end) {
                            tokensIndexes[i] = e
                        }
                    } else {
                        logger.warn { "entity mismatch for $text" }
                        return@exp
                    }
                }

                tokens.forEachIndexed { index, token ->
                    val entity = tokensIndexes[index]

                    val role = when {
                        entity == null -> "O"
                        index == 0 -> entity.definition.role
                        else -> {
                            //deal with adjacent entities
                            val alreadyKnown = entityRoleMap[entity]
                            if (alreadyKnown != null) {
                                alreadyKnown
                            } else {
                                val r = tokensIndexes[index - 1]
                                    ?.takeIf { entityRoleMap[it] == entity.definition.role }
                                    ?.let { "$ADJACENT_ENTITY_MARKER${it.definition.role}" }
                                    ?: entity.definition.role
                                entityRoleMap[entity] = r
                                r
                            }
                        }
                    }
                    sb.append(token)
                    sb.append(TAB)
                    sb.appendln(role)
                }
                sb.appendln()
                logger.trace { "$text ->\n$sb" }
            } catch (e: Exception) {
                logger.error("error with $expression", e)
                return@exp
            }
        }

        return sb.toString()
    }
}