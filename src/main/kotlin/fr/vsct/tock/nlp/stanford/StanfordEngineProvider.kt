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
import fr.vsct.tock.nlp.model.EntityClassifier
import fr.vsct.tock.nlp.model.IntentClassifier
import fr.vsct.tock.nlp.model.Tokenizer
import fr.vsct.tock.nlp.model.service.engine.EntityModelHolder
import fr.vsct.tock.nlp.model.service.engine.IntentModelHolder
import fr.vsct.tock.nlp.model.service.engine.NlpEngineModelBuilder
import fr.vsct.tock.nlp.model.service.engine.NlpEngineModelIo
import fr.vsct.tock.nlp.model.service.engine.NlpEngineProvider
import fr.vsct.tock.nlp.model.service.engine.TokenizerModelHolder

/**
 *
 */
class StanfordEngineProvider : NlpEngineProvider {

    override fun type(): NlpEngineType {
        return NlpEngineType.stanford
    }

    override fun getIntentClassifier(model: IntentModelHolder): IntentClassifier {
        return StanfordIntentClassifier(model)
    }

    override fun getEntityClassifier(model: EntityModelHolder): EntityClassifier {
        return StanfordEntityClassifier(model)
    }

    override fun getTokenizer(model: TokenizerModelHolder): Tokenizer {
        return StanfordTokenizer(model)
    }

    override fun getModelBuilder(): NlpEngineModelBuilder {
        return StanfordModelBuilder
    }

    override fun getModelIo(): NlpEngineModelIo {
        return StanfordModelIo
    }
}