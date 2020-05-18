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
import ai.tock.nlp.core.EntityRecognition
import ai.tock.nlp.core.EntityType
import ai.tock.nlp.core.EntityValue
import ai.tock.nlp.core.Intent
import ai.tock.nlp.core.NlpEngineType
import ai.tock.nlp.core.sample.SampleContext
import ai.tock.nlp.core.sample.SampleEntity
import ai.tock.nlp.core.sample.SampleExpression
import ai.tock.nlp.model.EntityBuildContextForIntent
import ai.tock.nlp.model.EntityCallContextForIntent
import ai.tock.nlp.model.TokenizerContext
import ai.tock.nlp.model.service.engine.EntityModelHolder
import ai.tock.nlp.model.service.engine.TokenizerModelHolder
import ai.tock.nlp.stanford.StanfordModelBuilder.defaultNlpApplicationConfiguration
import ai.tock.shared.defaultLocale
import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.ling.CoreLabel
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.test.assertEquals

/**
 *
 */
class StanfordEntityClassifierTest {

    private val tokenizer = StanfordTokenizer(TokenizerModelHolder(Locale.FRENCH, defaultNlpApplicationConfiguration()))
    private val tokenizerContext = TokenizerContext(Locale.FRENCH, NlpEngineType.stanford, "app")

    @Test
    fun classifyEntities_withEmptyEntityModel_shouldNotFail() {
        val classifier =
            StanfordEntityClassifier(
                EntityModelHolder(
                    CRFClassifier<CoreLabel>(StanfordModelBuilder.defaultEntityClassifierConfiguration.properties),
                    defaultNlpApplicationConfiguration()
                )
            )
        val context = EntityCallContextForIntent(
            Intent("test", emptyList()),
            defaultLocale,
            NlpEngineType.stanford,
            "test",
            ZonedDateTime.now()
        )

        classifier.classifyEntities(context, "test", arrayOf("test"))
    }

    @Test
    fun `classify entities with two adjacent entities returns two adjacent entities and not only one`() {
        val entityType = EntityType("type")
        val entity = Entity(entityType, "a")
        val intent = Intent("test", listOf(entity, Entity(entityType, "b")))
        val model = StanfordModelBuilder.buildEntityModel(
            EntityBuildContextForIntent(
                intent,
                Locale.FRENCH,
                NlpEngineType.stanford,
                "app"
            ),
            defaultNlpApplicationConfiguration(),
            listOf(
                SampleExpression(
                    "11/11 12/11",
                    Intent("intent", emptyList()),
                    listOf(
                        SampleEntity(
                            entity,
                            emptyList(),
                            0,
                            5
                        ),
                        SampleEntity(
                            entity,
                            emptyList(),
                            6,
                            11
                        )
                    ),
                    SampleContext()
                ),
                SampleExpression(
                    "12/11 13/11",
                    Intent("intent", emptyList()),
                    listOf(
                        SampleEntity(
                            entity,
                            emptyList(),
                            0,
                            5
                        ),
                        SampleEntity(
                            entity,
                            emptyList(),
                            6,
                            11
                        )
                    ),
                    SampleContext()
                ),
                SampleExpression(
                    "13/11 14/11",
                    Intent("intent", emptyList()),
                    listOf(
                        SampleEntity(
                            entity,
                            emptyList(),
                            0,
                            5
                        ),
                        SampleEntity(
                            entity,
                            emptyList(),
                            6,
                            11
                        )
                    ),
                    SampleContext()
                ),
                SampleExpression(
                    "15/11 16/11",
                    Intent("intent", emptyList()),
                    listOf(
                        SampleEntity(
                            entity,
                            emptyList(),
                            0,
                            5
                        ),
                        SampleEntity(
                            entity,
                            emptyList(),
                            6,
                            11
                        )
                    ),
                    SampleContext()
                )
            )
        )

        val classifier =
            StanfordEntityClassifier(
                model
            )
        val context = EntityCallContextForIntent(
            intent,
            defaultLocale,
            NlpEngineType.stanford,
            "test",
            ZonedDateTime.now()
        )

        val result = classifier.classifyEntities(context, "15/11 16/11", tokenizer.tokenize(tokenizerContext, "15/11 16/11"))

        assertEquals(
            listOf(
                EntityRecognition(EntityValue(0, 5, entity), .835),
                EntityRecognition(EntityValue(6, 11, entity), .835)
            ),
            result
        )
    }

    @Test
    fun `complex expression must not fail`() {
        val entityType = EntityType("type")
        val entity = Entity(entityType, "a")
        val entityB = Entity(entityType, "b")
        val intent = Intent("test", listOf(entity, entityB))
        val model = StanfordModelBuilder.buildEntityModel(
            EntityBuildContextForIntent(
                intent,
                Locale.FRENCH,
                NlpEngineType.stanford,
                "app"
            ),
            defaultNlpApplicationConfiguration(),
            listOf(
                SampleExpression(
                    "bordeaux st-jean_gare montparnasse  -paris  AR  le 20/27/2018 au 23/07/ 2018",
                    Intent("intent", emptyList()),
                    listOf(
                        SampleEntity(
                            Entity(entityType, "a"),
                            emptyList(),
                            0,
                            16
                        ),
                        SampleEntity(
                            entityB,
                            emptyList(),
                            17,
                            42
                        ),
                        SampleEntity(
                            entityB,
                            emptyList(),
                            44,
                            46
                        ),
                        SampleEntity(
                            Entity(entityType, "a"),
                            emptyList(),
                            48,
                            61
                        ),
                        SampleEntity(
                            entityB,
                            emptyList(),
                            65,
                            76
                        )
                    ),
                    SampleContext()
                ),
                SampleExpression(
                    "12/11 13/11",
                    Intent("intent", emptyList()),
                    listOf(
                        SampleEntity(
                            entity,
                            emptyList(),
                            0,
                            5
                        ),
                        SampleEntity(
                            entity,
                            emptyList(),
                            6,
                            11
                        )
                    ),
                    SampleContext()
                ),
                SampleExpression(
                    "13/11 14/11",
                    Intent("intent", emptyList()),
                    listOf(
                        SampleEntity(
                            entity,
                            emptyList(),
                            0,
                            5
                        ),
                        SampleEntity(
                            entity,
                            emptyList(),
                            6,
                            11
                        )
                    ),
                    SampleContext()
                ),
                SampleExpression(
                    "15/11 16/11",
                    Intent("intent", emptyList()),
                    listOf(
                        SampleEntity(
                            entity,
                            emptyList(),
                            0,
                            5
                        ),
                        SampleEntity(
                            entity,
                            emptyList(),
                            6,
                            11
                        )
                    ),
                    SampleContext()
                )
            )
        )

        val classifier =
            StanfordEntityClassifier(
                model
            )
        val context = EntityCallContextForIntent(
            intent,
            defaultLocale,
            NlpEngineType.stanford,
            "test",
            ZonedDateTime.now()
        )

        val result = classifier.classifyEntities(context, "bordeaux st-jean_gare montparnasse  -paris  AR  le 20/27/2018 au 23/07/ 2018", tokenizer.tokenize(tokenizerContext, "bordeaux st-jean_gare montparnasse  -paris  AR  le 20/27/2018 au 23/07/ 2018"))

        assertEquals(
            listOf(
                EntityRecognition(EntityValue(0, 16, entity), .958),
                EntityRecognition(EntityValue(17, 42, entityB), .958),
                EntityRecognition(EntityValue(44, 46, entityB), .958),
                EntityRecognition(EntityValue(48, 61, entity), .958),
                EntityRecognition(EntityValue(65, 76, entityB), .958)
            ),
            result
        )
    }
}