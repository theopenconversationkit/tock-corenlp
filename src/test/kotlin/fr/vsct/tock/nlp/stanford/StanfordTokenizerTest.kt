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

import fr.vsct.tock.nlp.core.NlpEngineType
import fr.vsct.tock.nlp.model.TokenizerContext
import fr.vsct.tock.nlp.model.service.engine.TokenizerModelHolder
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.test.assertEquals

/**
 *
 */
internal class StanfordTokenizerTest {

    val tokenizer = StanfordTokenizer(TokenizerModelHolder(Locale.FRENCH))
    val context = TokenizerContext(Locale.FRENCH, NlpEngineType.stanford)

    @Test
    fun tokenize_wordsWithDash_areSplitted() {
        val tokens = tokenizer.tokenize(context, "Paris-Lyon du 25 au 28 Février")
        assertEquals("Paris", tokens[0])
        assertEquals("Lyon", tokens[2])
        assertEquals(8, tokens.size)
    }

    @Test
    fun tokenize_wordsWithSimpleQuote_areSplitted() {
        val tokens = tokenizer.tokenize(context, "Cap d'Agde")
        assertEquals("Cap", tokens[0])
        assertEquals("d", tokens[1])
        assertEquals("'", tokens[2])
        assertEquals("Agde", tokens[3])
        assertEquals(4, tokens.size)
    }

    @Test
    fun `tokenize is taken upper case into account`() {
        val tokens = tokenizer.tokenize(context, "ParisNimes")
        assertEquals("Paris", tokens[0])
        assertEquals("Nimes", tokens[1])
    }

    @Test
    fun `tokenize handles special chars`() {
        var tokens = tokenizer.tokenize(context, "s.25/3")
        assertEquals("s", tokens[0])
        assertEquals(".", tokens[1])
        assertEquals("25", tokens[2])
        assertEquals("/", tokens[3])
        assertEquals("3", tokens[4])

        tokens = tokenizer.tokenize(context, "Aller:15/02")
        assertEquals("Aller", tokens[0])
        assertEquals(":", tokens[1])
        assertEquals("15", tokens[2])
        assertEquals("/", tokens[3])
        assertEquals("02", tokens[4])

        tokens = tokenizer.tokenize(context, "A-l'l#e,f")
        assertEquals("A", tokens[0])
        assertEquals("-", tokens[1])
        assertEquals("l", tokens[2])
        assertEquals("'", tokens[3])
        assertEquals("l", tokens[4])
        assertEquals("#", tokens[5])
        assertEquals("e", tokens[6])
        assertEquals(",", tokens[7])
        assertEquals("f", tokens[8])
    }

    @Test
    fun tokenize_spaceText_shouldNotFail() {
        val tokens = tokenizer.tokenize(context, " ")
        assertEquals(0, tokens.size)
    }

    @Test
    fun tokenize_specialChar_splitInOneToken() {
        val tokens = tokenizer.tokenize(context, "😀")
        assertEquals(1, tokens.size)
    }

    @Test
    fun tokenize_twoSpecialChars_splitInTwoTokens() {
        val tokens = tokenizer.tokenize(context, "😀 😀")
        assertEquals(2, tokens.size)
    }

    @Test
    fun tokenize_twoSpecialCharsAndClassicWord_splitInThreeTokens() {
        val tokens = tokenizer.tokenize(context, "😀😀 Paris")
        assertEquals(3, tokens.size)
        assertEquals("Paris", tokens[2])
    }

    @Test
    fun tokenize_StringWithSharp_shouldSplitTokens() {
        val tokens = tokenizer.tokenize(context, "#zea1")
        assertEquals(3, tokens.size)
        assertEquals("#", tokens[0])
        assertEquals("zea", tokens[1])
        assertEquals("1", tokens[2])
    }
}