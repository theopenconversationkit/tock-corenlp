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

import fr.vsct.tock.nlp.core.IntentRecognition
import fr.vsct.tock.nlp.model.IntentContext
import fr.vsct.tock.nlp.model.service.engine.IntentModelHolder
import fr.vsct.tock.nlp.model.service.engine.NlpIntentClassifier

/**
 *
 */
internal class StanfordIntentClassifier(model: IntentModelHolder) : NlpIntentClassifier(model) {

    override fun classifyIntent(context: IntentContext, text: String, tokens: Array<String>): List<IntentRecognition> {
        with(model) {
            if (!model.application.intents.isEmpty()) {
                with(nativeModel as StanfordIntentModel) {
                    val d = cdc.makeDatumFromLine("\t$text")
                    return classifier.scoresOf(d)
                            .entrySet()
                            .map {
                                IntentRecognition(application.getIntent(it.key), it.value)
                            }
                }
            }
            return emptyList()
        }
    }
}