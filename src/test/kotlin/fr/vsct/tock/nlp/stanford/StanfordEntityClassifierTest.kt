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
import fr.vsct.tock.nlp.core.Entity
import fr.vsct.tock.nlp.core.EntityRecognition
import fr.vsct.tock.nlp.core.EntityType
import fr.vsct.tock.nlp.core.EntityValue
import fr.vsct.tock.nlp.core.Intent
import fr.vsct.tock.nlp.core.NlpEngineType
import fr.vsct.tock.nlp.core.sample.SampleContext
import fr.vsct.tock.nlp.core.sample.SampleEntity
import fr.vsct.tock.nlp.core.sample.SampleExpression
import fr.vsct.tock.nlp.model.EntityBuildContextForIntent
import fr.vsct.tock.nlp.model.EntityCallContextForIntent
import fr.vsct.tock.nlp.model.TokenizerContext
import fr.vsct.tock.nlp.model.service.engine.EntityModelHolder
import fr.vsct.tock.nlp.model.service.engine.TokenizerModelHolder
import fr.vsct.tock.nlp.stanford.StanfordModelBuilder.defaultNlpApplicationConfiguration
import fr.vsct.tock.shared.defaultLocale
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
}