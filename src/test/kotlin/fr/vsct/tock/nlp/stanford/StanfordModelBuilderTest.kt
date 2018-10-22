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

import fr.vsct.tock.nlp.core.Entity
import fr.vsct.tock.nlp.core.EntityType
import fr.vsct.tock.nlp.core.Intent
import fr.vsct.tock.nlp.core.NlpEngineType
import fr.vsct.tock.nlp.core.sample.SampleContext
import fr.vsct.tock.nlp.core.sample.SampleEntity
import fr.vsct.tock.nlp.core.sample.SampleExpression
import fr.vsct.tock.nlp.model.EntityBuildContextForEntity
import fr.vsct.tock.nlp.model.EntityBuildContextForIntent
import fr.vsct.tock.nlp.stanford.StanfordModelBuilder.getEntityTrainData
import fr.vsct.tock.shared.defaultLocale
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.streams.toList
import kotlin.test.assertEquals

/**
 *
 */
class StanfordModelBuilderTest {

    @Test
    fun getEntityTrainData_withNonTrimmedExpression_ShouldNotFail() {
        val data = getEntityTrainData(
            EntityBuildContextForEntity(EntityType("test"), defaultLocale, NlpEngineType.stanford),
            listOf(
                SampleExpression(
                    " test",
                    Intent("intent", emptyList()),
                    listOf(
                        SampleEntity(
                            Entity(EntityType("test"), "test"),
                            emptyList(),
                            1,
                            5
                        )
                    ),
                    SampleContext()
                )
            )
        )
        assertEquals("test	test", data.readLine())
    }

    @Test
    fun `getEntityTrainData with same tokens is ok`() {
        val entityType = EntityType("type")
        val data = getEntityTrainData(
            EntityBuildContextForIntent(
                "app",
                Intent("test", listOf(Entity(entityType, "a"), Entity(entityType, "b"))),
                Locale.FRENCH,
                NlpEngineType.stanford
            ),
            listOf(
                SampleExpression(
                    "11/11 au 12/11",
                    Intent("intent", emptyList()),
                    listOf(
                        SampleEntity(
                            Entity(entityType, "a"),
                            emptyList(),
                            0,
                            5
                        ),
                        SampleEntity(
                            Entity(entityType, "b"),
                            emptyList(),
                            9,
                            14
                        )
                    ),
                    SampleContext()
                )
            )
        )

        assertEquals(
            listOf("11\ta", "/\ta", "11\ta", "au\tO", "12\tb", "/\tb", "11\tb", ""),
            data.lines().toList()
        )
    }

    //Aller:15/02 - Retour:17/02
    //22/3 /2018 retours.25/3 / 2018
    //ParisNimes
}