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

import ai.tock.nlp.core.Entity
import ai.tock.nlp.core.EntityType
import ai.tock.nlp.core.Intent
import ai.tock.nlp.core.NlpEngineType
import ai.tock.nlp.core.sample.SampleContext
import ai.tock.nlp.core.sample.SampleEntity
import ai.tock.nlp.core.sample.SampleExpression
import ai.tock.nlp.model.EntityBuildContextForEntity
import ai.tock.nlp.model.EntityBuildContextForIntent
import ai.tock.nlp.stanford.StanfordModelBuilder.defaultNlpApplicationConfiguration
import ai.tock.nlp.stanford.StanfordModelBuilder.getEntityTrainData
import ai.tock.shared.defaultLocale
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.test.assertEquals

/**
 *
 */
class StanfordModelBuilderTest {
    @Test
    fun getEntityTrainData_withNonTrimmedExpression_ShouldNotFail() {
        val data =
            getEntityTrainData(
                EntityBuildContextForEntity(EntityType("test"), defaultLocale, NlpEngineType.stanford, "app"),
                defaultNlpApplicationConfiguration(),
                listOf(
                    SampleExpression(
                        " test",
                        Intent("intent", emptyList()),
                        listOf(
                            SampleEntity(
                                Entity(EntityType("test"), "test"),
                                emptyList(),
                                1,
                                5,
                            ),
                        ),
                        SampleContext(),
                    ),
                ),
            )
        assertEquals("test	test", data.split("\n").first())
    }

    @Test
    fun `getEntityTrainData with same tokens is ok`() {
        val entityType = EntityType("type")
        val data =
            getEntityTrainData(
                EntityBuildContextForIntent(
                    Intent("test", listOf(Entity(entityType, "a"), Entity(entityType, "b"))),
                    Locale.FRENCH,
                    NlpEngineType.stanford,
                    "app",
                ),
                defaultNlpApplicationConfiguration(),
                listOf(
                    SampleExpression(
                        "11/11 au 12/11",
                        Intent("intent", emptyList()),
                        listOf(
                            SampleEntity(
                                Entity(entityType, "a"),
                                emptyList(),
                                0,
                                5,
                            ),
                            SampleEntity(
                                Entity(entityType, "b"),
                                emptyList(),
                                9,
                                14,
                            ),
                        ),
                        SampleContext(),
                    ),
                ),
            )

        assertEquals(
            listOf("11\ta", "/\ta", "11\ta", "au\tO", "12\tb", "/\tb", "11\tb", "", ""),
            data.split("\n").toList(),
        )
    }

    @Test
    fun `separator of two train data is one line separator`() {
        val entityType = EntityType("type")
        val data =
            getEntityTrainData(
                EntityBuildContextForIntent(
                    Intent("test", listOf(Entity(entityType, "a"), Entity(entityType, "b"))),
                    Locale.FRENCH,
                    NlpEngineType.stanford,
                    "app",
                ),
                defaultNlpApplicationConfiguration(),
                listOf(
                    SampleExpression(
                        "11/11 au 12/11",
                        Intent("intent", emptyList()),
                        listOf(
                            SampleEntity(
                                Entity(entityType, "a"),
                                emptyList(),
                                0,
                                5,
                            ),
                            SampleEntity(
                                Entity(entityType, "b"),
                                emptyList(),
                                9,
                                14,
                            ),
                        ),
                        SampleContext(),
                    ),
                    SampleExpression(
                        "ok",
                        Intent("intent", emptyList()),
                        listOf(),
                        SampleContext(),
                    ),
                ),
            )

        assertEquals(
            listOf("11\ta", "/\ta", "11\ta", "au\tO", "12\tb", "/\tb", "11\tb", "", "ok\tO", "", ""),
            data.split("\n").toList(),
        )
    }
}
