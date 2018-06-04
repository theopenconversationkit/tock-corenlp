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

import edu.stanford.nlp.stats.Counters
import fr.vsct.tock.nlp.core.Intent
import fr.vsct.tock.nlp.core.IntentClassification
import fr.vsct.tock.nlp.model.IntentContext
import fr.vsct.tock.nlp.model.service.engine.IntentModelHolder
import fr.vsct.tock.nlp.model.service.engine.NlpIntentClassifier

/**
 *
 */
internal class StanfordIntentClassifier(model: IntentModelHolder) : NlpIntentClassifier(model) {

    companion object {
        val emptyClassification = object : IntentClassification {
            override fun probability(): Double = 0.0

            override fun hasNext(): Boolean = false

            override fun next(): Intent = throw NoSuchElementException()
        }
    }

    override fun classifyIntent(context: IntentContext, text: String, tokens: Array<String>): IntentClassification {
        return with(model) {
            if (!model.application.intents.isEmpty()) {
                with(nativeModel as StanfordIntentModel) {
                    val d = cdc.makeDatumFromLine("\t$text")
                    val scores = classifier.scoresOf(d)
                    val logSum = Counters.logSum(scores)
                    val iterator = scores.entrySet().sortedByDescending { it.value }.iterator()
                    return object : IntentClassification {

                        var probability = 0.0

                        override fun probability(): Double = probability

                        override fun hasNext(): Boolean = iterator.hasNext()

                        override fun next(): Intent {
                            return iterator.next().let { (key, proba) ->
                                probability = Math.exp(proba - logSum)
                                application.getIntent(key) ?: Intent.UNKNOWN_INTENT
                            }
                        }
                    }
                }
            } else {
                emptyClassification
            }
        }
    }

}