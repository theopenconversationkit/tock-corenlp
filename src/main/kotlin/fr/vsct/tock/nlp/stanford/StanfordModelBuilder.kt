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
import fr.vsct.tock.nlp.core.sample.SampleExpression
import fr.vsct.tock.nlp.model.EntityBuildContext
import fr.vsct.tock.nlp.model.IntentContext
import fr.vsct.tock.nlp.model.TokenizerContext
import fr.vsct.tock.nlp.model.service.engine.EntityModelHolder
import fr.vsct.tock.nlp.model.service.engine.IntentModelHolder
import fr.vsct.tock.nlp.model.service.engine.NlpEngineModelBuilder
import fr.vsct.tock.nlp.model.service.engine.TokenizerModelHolder
import fr.vsct.tock.shared.resourceAsStream
import mu.KotlinLogging
import java.io.BufferedReader
import java.io.StringReader
import java.lang.StringBuilder
import java.time.Instant
import java.util.Properties
import java.util.stream.Collectors

/**
 *
 */
internal object StanfordModelBuilder : NlpEngineModelBuilder {

    private val logger = KotlinLogging.logger {}

    const val TAB = "\t"

    override fun buildTokenizerModel(
        context: TokenizerContext,
        expressions: List<SampleExpression>
    ): TokenizerModelHolder {
        return TokenizerModelHolder(context.language)
    }

    fun intentClassifierProperties(): Properties {
        return Properties().apply { load(resourceAsStream("/stanford/intentClassifier.prop")) }
    }

    fun entityClassifierProperties(): Properties {
        return Properties().apply { load(resourceAsStream("/stanford/crfclassifier.prop")) }
    }


    override fun buildIntentModel(context: IntentContext, expressions: List<SampleExpression>): IntentModelHolder {
        val properties = intentClassifierProperties()
        val cdc = ColumnDataClassifier(properties)
        val dataset = Dataset<String, String>()
        expressions.map {
            dataset.add(cdc.makeDatumFromLine("${it.intent.name}${TAB}$${it.text}"))
        }
        val classifier = cdc.makeClassifier(dataset)
        return IntentModelHolder(context.application, StanfordIntentModel(cdc, classifier), Instant.now())
    }

    override fun buildEntityModel(context: EntityBuildContext, expressions: List<SampleExpression>): EntityModelHolder {
        val crfClassifier = CRFClassifier<CoreLabel>(entityClassifierProperties())
        val trainingData = getEntityTrainData(context, expressions)
        try {
            val transformedData: ObjectBank<MutableList<CoreLabel>> = crfClassifier.makeObjectBankFromReader(
                trainingData.second,
                crfClassifier.defaultReaderAndWriter()
            )
            crfClassifier.train(transformedData)
            return EntityModelHolder(crfClassifier, Instant.now())
        } catch (e: Exception) {
            logger.error {
                "error with train data: \n ${getEntityTrainData(context, expressions).second.lines().collect(
                    Collectors.joining("\n")
                )}"
            }
            throw e
        }
    }

    internal fun getEntityTrainData(
        context: EntityBuildContext,
        expressions: List<SampleExpression>
    ): Pair<List<String>, BufferedReader> {
        val tokenizer = StanfordEngineProvider.getTokenizer(TokenizerModelHolder(context.language))
        val tokenizerContext = TokenizerContext(context)
        val list = mutableListOf<String>()
        val sb = StringBuilder()
        expressions.forEach { expression ->
            val text = expression.text
            if (text.contains("\n") || text.contains("\t")) {
                logger.warn { "expression $text contains \\n or \\t!!! - skipped" }
                return@forEach
            }
            val tokens = tokenizer.tokenize(tokenizerContext, text)
            val tokenMap = expression.entities.map { e ->
                val start =
                    if (e.start == 0) 0 else tokenizer.tokenize(tokenizerContext, text.substring(0, e.start)).size
                val end = start + tokenizer.tokenize(tokenizerContext, text.substring(e.start, e.end)).size
                if (start >= tokens.size || end > tokens.size) {
                    emptyList()
                } else {
                    (start until end).map {
                        tokens[it] to e.definition
                    }
                }
            }.flatMap { it }.toMap()

            tokens.forEach { token ->
                val entity = tokenMap.get(token)
                if (entity != null) {
                    sb.appendln("${token}${TAB}${entity.role}")
                } else {
                    sb.appendln("${token}${TAB}O")
                }
            }
            list.add(text)
            sb.appendln()
        }
        return Pair(list, BufferedReader(StringReader(sb.toString())))
    }
}