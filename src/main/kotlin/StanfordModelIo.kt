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

import edu.stanford.nlp.classify.Classifier
import edu.stanford.nlp.classify.ColumnDataClassifier
import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.ling.CoreLabel
import ai.tock.nlp.model.service.engine.NlpEngineModelIo
import ai.tock.nlp.model.service.storage.NlpModelStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream

/**
 *
 */
internal object StanfordModelIo : NlpEngineModelIo {

    override fun loadTokenizerModel(input: NlpModelStream): Any {
        TODO()
    }

    override fun loadIntentModel(input: NlpModelStream): Any =
        ObjectInputStream(input.inputStream).use {
            @Suppress("UNCHECKED_CAST")
            val classifier = it.readObject() as Classifier<String, String>
            StanfordIntentModel(
                ColumnDataClassifier(
                    input.configuration?.intentConfiguration?.properties
                            ?: StanfordModelBuilder.defaultIntentClassifierConfiguration.properties
                )
                , classifier
            )
        }

    override fun loadEntityModel(input: NlpModelStream): Any =
        input.inputStream.use { stream ->
            CRFClassifier<CoreLabel>(
                input.configuration?.entityConfiguration?.properties
                        ?: StanfordModelBuilder.defaultEntityClassifierConfiguration.properties
            )
                .apply { loadClassifier(stream) }
        }

    override fun copyTokenizerModel(model: Any, output: OutputStream) {
        TODO()
    }

    override fun copyIntentModel(model: Any, output: OutputStream) {
        val stanfordModel = model as StanfordIntentModel
        val objectOutputStream = ObjectOutputStream(output)
        objectOutputStream.use {
            objectOutputStream.writeObject(stanfordModel.classifier)
        }
    }

    override fun copyEntityModel(model: Any, output: OutputStream) {
        val crfClassifier = model as CRFClassifier<*>
        val objectOutputStream = ObjectOutputStream(output)
        objectOutputStream.use {
            crfClassifier.serializeClassifier(objectOutputStream)
        }
    }
}