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

import edu.stanford.nlp.international.french.process.FrenchTokenizer
import edu.stanford.nlp.international.spanish.process.SpanishTokenizer
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.process.PTBTokenizer
import edu.stanford.nlp.process.TokenizerFactory
import fr.vsct.tock.nlp.model.TokenizerContext
import fr.vsct.tock.nlp.model.service.engine.NlpTokenizer
import fr.vsct.tock.nlp.model.service.engine.TokenizerModelHolder
import mu.KotlinLogging
import java.io.StringReader
import java.util.*

/**
 *
 */
internal class StanfordTokenizer(model: TokenizerModelHolder) : NlpTokenizer(model) {

    private val logger = KotlinLogging.logger {}

    val tokenizerFactory = getTokenizerFactory(model.language)
    val separators = arrayListOf("-", "'", "/")

    override fun tokenize(context: TokenizerContext, text: String): Array<String> {
        var rawTokens = tokenizerFactory.getTokenizer(StringReader(text)).tokenize().flatMap {
            coreLabel ->
            val word = coreLabel.word()
            splitSeparators(word)
        }
        if (rawTokens.isEmpty()) {
            rawTokens = if (text.trim().isEmpty()) {
                emptyList()
            } else {
                logger.warn { "empty token list for $text, do not split" }
                listOf(text.trim())
            }
        }

        return rawTokens.toTypedArray()
    }

    fun splitSeparators(word: String): List<String> {
        fun splitSeparator(words: List<String>, separator: String): List<String> {
            return words.flatMap { w ->
                if (w.length == 1) {
                    listOf(w)
                } else {
                    val index = w.indexOf(separator)
                    if (index == -1) {
                        listOf(w)
                    } else {
                        val split = word.split(separator)
                        return split.mapIndexed { i, s ->
                            listOfNotNull(
                                    if (i != 0) separator else null,
                                    if (s.isNotEmpty()) s else null)
                        }.flatMap { it }
                    }
                }
            }
        }

        var result = listOf(word)
        separators.forEach {
            result = splitSeparator(result, it)
        }
        return result
    }

    fun getTokenizerFactory(language: Locale): TokenizerFactory<CoreLabel> {
        logger.trace { "getting tokenizer for : $language" }
        return when (language.language) {
            "fr" -> {
                FrenchTokenizer.FrenchTokenizerFactory.newTokenizerFactory()
                        .also {
                            it.setOptions("untokenizable=noneDelete")
                        }
            }
            "en" -> {
                PTBTokenizer.PTBTokenizerFactory.newCoreLabelTokenizerFactory("")
            }
            "es" -> {
                SpanishTokenizer.SpanishTokenizerFactory.newCoreLabelTokenizerFactory()
            }
            else -> {
                PTBTokenizer.PTBTokenizerFactory.newCoreLabelTokenizerFactory("")
            }
        }
    }
}