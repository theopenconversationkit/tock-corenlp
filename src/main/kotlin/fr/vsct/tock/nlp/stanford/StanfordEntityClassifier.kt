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

import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.ling.CoreLabel
import ai.tock.nlp.core.Entity
import ai.tock.nlp.core.EntityRecognition
import ai.tock.nlp.core.EntityValue
import ai.tock.nlp.core.IntOpenRange
import ai.tock.nlp.model.EntityCallContext
import ai.tock.nlp.model.EntityCallContextForEntity
import ai.tock.nlp.model.EntityCallContextForIntent
import ai.tock.nlp.model.EntityCallContextForSubEntities
import ai.tock.nlp.model.service.engine.EntityModelHolder
import ai.tock.nlp.model.service.engine.NlpEntityClassifier
import ai.tock.nlp.stanford.StanfordModelBuilder.ADJACENT_ENTITY_MARKER
import ai.tock.nlp.stanford.StanfordModelBuilder.TAB
import mu.KotlinLogging
import java.util.Arrays


internal class StanfordEntityClassifier(model: EntityModelHolder) : NlpEntityClassifier(model) {

    companion object {
        private val logger = KotlinLogging.logger {}
        private val adjacentMarkerRegep = ADJACENT_ENTITY_MARKER.toRegex()
    }

    private data class Token(
        override val start: Int,
        override val end: Int,
        val text: String,
        val type: String
    ) : IntOpenRange

    override fun classifyEntities(
        context: EntityCallContext,
        text: String,
        tokens: Array<String>
    ): List<EntityRecognition> {
        return when (context) {
            is EntityCallContextForIntent -> classifyEntities(context, text, tokens)
            is EntityCallContextForEntity -> TODO()
            is EntityCallContextForSubEntities -> classifyEntities(context, text, tokens)
        }
    }

    private fun classifyEntities(
        context: EntityCallContextForSubEntities,
        text: String,
        tokens: Array<String>
    ): List<EntityRecognition> {
        return classifyEntities(text, tokens) { context.entityType.findSubEntity(it) }
    }

    private fun classifyEntities(
        context: EntityCallContextForIntent,
        text: String,
        tokens: Array<String>
    ): List<EntityRecognition> {
        return classifyEntities(text, tokens) { context.intent.getEntity(it) }
    }

    private fun classifyEntities(
        text: String,
        tokens: Array<String>,
        entityFinder: (String) -> Entity?
    ): List<EntityRecognition> {
        return try {
            with(model) {
                @Suppress("UNCHECKED_CAST")
                val classifier = nativeModel as CRFClassifier<CoreLabel>

                val evaluationData = getEvaluationData(tokens)
                val documents = classifier.makeObjectBankFromString(evaluationData, classifier.defaultReaderAndWriter())
                val document = documents.flatten()

                val classifiedLabels = classifier.classify(document)

                val confidence = getConfidence(classifier, classifiedLabels)

                val coreTokens = mutableListOf<Token>()
                var previousToken: Token? = null
                document.forEachIndexed { index, word ->
                    var t = text
                    var start = 0
                    for (i in 0 until index) {
                        val nextTokenIndex = document[i].word().length + t.indexOf(document[i].word())
                        start += nextTokenIndex
                        t = t.substring(nextTokenIndex)
                    }

                    start += t.indexOf(document[index].word())
                    val end = start + document[index].word().length

                    val entityRole = word.get(CoreAnnotations.AnswerAnnotation::class.java)
                    if (entityRole != "O") {
                        if (previousToken?.type != entityRole) {
                            previousToken = Token(start, end, word.word(), entityRole)
                        } else {
                            coreTokens.removeAt(coreTokens.lastIndex)
                            val w = text.substring((previousToken as Token).start, end)
                            previousToken = Token((previousToken as Token).start, end, w, entityRole)
                        }

                        val tok = previousToken!!

                        coreTokens.add(Token(tok.start, tok.end, tok.text, tok.type))
                    } else {
                        previousToken = null
                    }
                }

                coreTokens.mapNotNull {
                    val entity = entityFinder.invoke(it.type.replaceFirst(adjacentMarkerRegep, ""))
                    if (entity == null) {
                        logger.warn { "unknown entity role ${it.type}" }
                        null
                    } else {
                        EntityRecognition(EntityValue(it.start, it.end, entity), confidence)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("error with $text and ${Arrays.toString(tokens)}", e)
            emptyList()
        }
    }

    private fun getEvaluationData(tokens: Array<String>): String =
        tokens.joinToString(separator = "") { "$it${TAB}O\n" }

    private fun getConfidence(classifier: CRFClassifier<CoreLabel>, classifiedLabels: List<CoreLabel>): Double {
        try {
            //TODO confidence by entity
            var counter = 0
            var probSum = 0.0
            val cliqueTree = classifier.getCliqueTree(classifiedLabels)
            for (i in 0 until cliqueTree.length()) {
                val wi = classifiedLabels[i]
                val index = classifier.classIndex.indexOf(wi.get(CoreAnnotations.AnswerAnnotation::class.java))
                probSum = cliqueTree.prob(i, index)
                counter++
            }
            val prob = 1 - (probSum / counter)
            return Math.round(prob * 1000) / 1000.0
        } catch (e: Exception) {
            logger.error(e) { "Exception during confidence calculation - skipped" }
            return 0.1
        }
    }
}