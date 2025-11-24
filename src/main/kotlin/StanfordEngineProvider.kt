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

import ai.tock.nlp.core.NlpEngineType
import ai.tock.nlp.model.service.engine.EntityClassifier
import ai.tock.nlp.model.service.engine.EntityModelHolder
import ai.tock.nlp.model.service.engine.IntentClassifier
import ai.tock.nlp.model.service.engine.IntentModelHolder
import ai.tock.nlp.model.service.engine.NlpEngineModelBuilder
import ai.tock.nlp.model.service.engine.NlpEngineModelIo
import ai.tock.nlp.model.service.engine.NlpEngineProvider
import ai.tock.nlp.model.service.engine.Tokenizer
import ai.tock.nlp.model.service.engine.TokenizerModelHolder

/**
 *
 */
class StanfordEngineProvider : NlpEngineProvider {
    companion object {
        fun getStanfordTokenizer(model: TokenizerModelHolder): Tokenizer {
            return StanfordTokenizer(model)
        }
    }

    override val type: NlpEngineType = NlpEngineType.stanford

    override fun getIntentClassifier(model: IntentModelHolder): IntentClassifier {
        return StanfordIntentClassifier(model)
    }

    override fun getEntityClassifier(model: EntityModelHolder): EntityClassifier {
        return StanfordEntityClassifier(model)
    }

    override fun getTokenizer(model: TokenizerModelHolder): Tokenizer {
        return getStanfordTokenizer(model)
    }

    override val modelBuilder: NlpEngineModelBuilder = StanfordModelBuilder

    override val modelIo: NlpEngineModelIo = StanfordModelIo
}
