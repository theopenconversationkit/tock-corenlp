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

import edu.stanford.nlp.classify.Classifier
import edu.stanford.nlp.classify.ColumnDataClassifier
import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.ling.CoreLabel
import fr.vsct.tock.nlp.model.service.engine.NlpEngineModelIo
import fr.vsct.tock.nlp.model.service.storage.NlpModelStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.util.Properties

/**
 *
 */
internal object StanfordModelIo : NlpEngineModelIo {

    override fun loadTokenizerModel(input: NlpModelStream): Any {
        TODO()
    }

    override fun loadIntentModel(input: NlpModelStream): Any {
        val objectStream = ObjectInputStream(input.inputStream)
        @Suppress("UNCHECKED_CAST")
        val classifier = objectStream.readObject() as Classifier<String, String>
        val properties = objectStream.readObject() as Properties
        return StanfordIntentModel(ColumnDataClassifier(properties), classifier)
    }

    override fun loadEntityModel(input: NlpModelStream): Any {
        val crfClassifier = CRFClassifier<CoreLabel>(StanfordModelBuilder.entityClassifierProperties())
        crfClassifier.loadClassifier(input.inputStream)
        return crfClassifier
    }

    override fun copyTokenizerModel(model: Any, output: OutputStream) {
        TODO()
    }

    override fun copyIntentModel(model: Any, output: OutputStream) {
        val stanfordModel = model as StanfordIntentModel
        val objectOutputStream = ObjectOutputStream(output)
        objectOutputStream.use {
            objectOutputStream.writeObject(stanfordModel.classifier)
            objectOutputStream.writeObject(StanfordModelBuilder.intentClassifierProperties())
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