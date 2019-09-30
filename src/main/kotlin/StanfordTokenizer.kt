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

import edu.stanford.nlp.international.french.process.FrenchTokenizer
import edu.stanford.nlp.international.spanish.process.SpanishTokenizer
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.process.PTBTokenizer
import edu.stanford.nlp.process.TokenizerFactory
import ai.tock.nlp.model.TokenizerContext
import ai.tock.nlp.model.service.engine.NlpTokenizer
import ai.tock.nlp.model.service.engine.TokenizerModelHolder
import ai.tock.shared.error
import mu.KotlinLogging
import java.io.StringReader
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 *
 */
internal class StanfordTokenizer(model: TokenizerModelHolder) : NlpTokenizer(model) {

    companion object {
        private val logger = KotlinLogging.logger {}
        private val separatorRegexpMap = ConcurrentHashMap<String, Regex>()
        private fun getTokenizerFactory(language: Locale): TokenizerFactory<CoreLabel> {
            logger.trace { "getting tokenizer for : $language" }
            return when (language.language) {
                "fr" -> {
                    FrenchTokenizer.FrenchTokenizerFactory.newTokenizerFactory()
                        .also {
                            it.setOptions("untokenizable=noneDelete")
                            //workaround stanford incapacity to disable splitContractionOption
                            FrenchTokenizer.FrenchTokenizerFactory::class.java.getDeclaredField("splitContractionOption")
                                .apply {
                                    isAccessible = true
                                    set(it, false)
                                }
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

    private val tokenizerFactory = getTokenizerFactory(model.language)

    override fun tokenize(context: TokenizerContext, text: String): Array<String> {
        val rawTokens = tokenizerFactory.getTokenizer(StringReader(text)).tokenize().flatMap { coreLabel ->
            val word = coreLabel.word()
            splitSeparators(
                word,
                model.configuration.tokenizerConfiguration.properties.getProperty("tock_stanford_tokens_separators")
            )
        }.let {
            if (it.isEmpty()) {
                if (text.trim().isEmpty()) {
                    emptyList()
                } else {
                    logger.warn { "empty token list for $text, do not split" }
                    listOf(text.trim())
                }
            } else {
                it
            }
        }

        logger.debug { rawTokens }

        return rawTokens.toTypedArray()
    }

    private fun separatorRegex(separators: String): Regex =
        separatorRegexpMap.getOrPut(separators) {
            logger.info { "using token separators: $separators" }
            val s: List<String> = separators
                .replace("\\,", "_comma_")
                .split(",")
                .map { it.replace("_comma_", ",") }
            s.joinToString("|").toRegex()
        }

    private fun splitSeparators(word: String, separators: String): List<String> {
        return try {
            separatorRegex(separators).replace(word) {
                //if more than one char, than a space between chars to split the whole separator
                it.value.run { if (length == 1) " $this " else toCharArray().joinToString(" ") }
            }
                .trim()
                .split(" ")
                .toList()
        } catch (e: Exception) {
            logger.error(e)
            listOf(word)
        }
    }


}