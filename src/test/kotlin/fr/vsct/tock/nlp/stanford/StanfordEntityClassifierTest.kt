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

import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.ling.CoreLabel
import fr.vsct.tock.nlp.core.Intent
import fr.vsct.tock.nlp.core.NlpEngineType
import fr.vsct.tock.nlp.model.EntityCallContextForIntent
import fr.vsct.tock.nlp.model.service.engine.EntityModelHolder
import fr.vsct.tock.shared.defaultLocale
import org.junit.Test
import java.time.ZonedDateTime

/**
 *
 */
class StanfordEntityClassifierTest {

    @Test
    fun classifyEntities_withEmptyEntityModel_shouldNotFail() {
        val classifier = StanfordEntityClassifier(EntityModelHolder(CRFClassifier<CoreLabel>(StanfordModelBuilder.entityClassifierProperties())))
        val context = EntityCallContextForIntent(
                "test",
                Intent("test", emptyList()),
                defaultLocale,
                NlpEngineType.stanford,
                ZonedDateTime.now())

        classifier.classifyEntities(context, "test", arrayOf("test"))
    }
}