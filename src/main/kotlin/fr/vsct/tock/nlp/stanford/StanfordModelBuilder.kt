/*
 *  This file is part of the tock-corenlp distribution.
 *  (https://github.com/voyages-sncf-technologies/tock-corenlp)
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

package fr.vsct.tock.nlp.stanford

import edu.stanford.nlp.classify.ColumnDataClassifier
import edu.stanford.nlp.classify.Dataset
import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.objectbank.ObjectBank
import fr.vsct.tock.nlp.core.Entity
import fr.vsct.tock.nlp.core.configuration.NlpApplicationConfiguration
import fr.vsct.tock.nlp.core.configuration.NlpModelConfiguration
import fr.vsct.tock.nlp.core.sample.SampleExpression
import fr.vsct.tock.nlp.model.EntityBuildContext
import fr.vsct.tock.nlp.model.IntentContext
import fr.vsct.tock.nlp.model.TokenizerContext
import fr.vsct.tock.nlp.model.service.engine.EntityModelHolder
import fr.vsct.tock.nlp.model.service.engine.IntentModelHolder
import fr.vsct.tock.nlp.model.service.engine.NlpEngineModelBuilder
import fr.vsct.tock.nlp.model.service.engine.TokenizerModelHolder
import fr.vsct.tock.shared.loadProperties
import mu.KotlinLogging
import java.io.BufferedReader
import java.io.StringReader
import kotlin.streams.toList

/**
 *
 */
internal object StanfordModelBuilder : NlpEngineModelBuilder {

    private val logger = KotlinLogging.logger {}

    const val TAB = "\t"

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
            val transformedData: ObjectBank<MutableList<CoreLabel>> = crfClassifier.makeObjectBankFromReader(
                trainingData,
                crfClassifier.defaultReaderAndWriter()
            )
            crfClassifier.train(transformedData)
            return EntityModelHolder(crfClassifier, configuration)
        } catch (e: Exception) {
            logger.error {
                "error with train data: \n ${getEntityTrainData(context, configuration, expressions).lines().toList()}"
            }
            throw e
        }
    }

    internal fun getEntityTrainData(
        context: EntityBuildContext,
        configuration: NlpApplicationConfiguration,
        expressions: List<SampleExpression>
    ): BufferedReader {
        val tokenizer =
            StanfordEngineProvider.getStanfordTokenizer(TokenizerModelHolder(context.language, configuration))
        val tokenizerContext = TokenizerContext(context)
        val sb = StringBuilder()
        val tokensIndexes: MutableMap<Int, Entity> = HashMap()

        expressions.forEach exp@{ expression ->
            val text = expression.text
            if (text.contains("\n") || text.contains("\t")) {
                logger.warn { "expression $text contains \\n or \\t!!! - skipped" }
                return@exp
            }
            tokensIndexes.clear()
            val tokens = tokenizer.tokenize(tokenizerContext, text)
            expression.entities.forEach { e ->
                val start =
                    if (e.start == 0) 0 else tokenizer.tokenize(tokenizerContext, text.substring(0, e.start)).size
                val end = start + tokenizer.tokenize(tokenizerContext, text.substring(e.start, e.end)).size
                if (start < tokens.size && end <= tokens.size) {
                    for (i in start until end) {
                        tokensIndexes[i] = e.definition
                    }
                } else {
                    logger.warn { "entity mismatch for $text" }
                    return@exp
                }
            }

            tokens.forEachIndexed { index, token ->
                sb.append(token)
                sb.append(TAB)
                val entity = tokensIndexes[index]
                sb.appendln(entity?.role ?: "O")
            }
            sb.appendln()
            logger.trace { "$text ->\n$sb" }
        }
        return BufferedReader(StringReader(sb.toString()))
    }
}