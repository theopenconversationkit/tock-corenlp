package fr.vsct.tock.nlp.stanford

import fr.vsct.tock.nlp.core.Application
import fr.vsct.tock.nlp.core.Entity
import fr.vsct.tock.nlp.core.EntityType
import fr.vsct.tock.nlp.core.Intent
import fr.vsct.tock.nlp.core.NlpEngineType
import fr.vsct.tock.nlp.integration.IntegrationConfiguration
import fr.vsct.tock.nlp.model.IntentContext
import fr.vsct.tock.nlp.model.TokenizerContext
import fr.vsct.tock.nlp.model.service.engine.TokenizerModelHolder
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 *
 */
class StanfordIntentClassifierTest {

    private val language = Locale.ENGLISH
    private val tokenizer = StanfordTokenizer(
        TokenizerModelHolder(
            language,
            StanfordModelBuilder.defaultNlpApplicationConfiguration()
        )
    )

    @Test
    fun classifyIntent_shouldReturnsAllIntentsAvailable() {
        val dump = IntegrationConfiguration.loadDump(NlpEngineType.stanford)
        with(dump) {
            val sentence = "this is a hard day"
            val application = Application(
                application.name,
                intents.map { definition ->
                    Intent(
                        definition.qualifiedName,
                        definition.entities.map { Entity(EntityType(it.entityTypeName), it.role) })
                },
                setOf(language)
            )
            val context = IntentContext(application, language, NlpEngineType.stanford)
            val expressions = sentences.map { s ->
                s.toSampleExpression(
                    {
                        intents.first { intent -> intent._id == s.classification.intentId }
                            .let { definition ->
                                Intent(
                                    definition.qualifiedName,
                                    definition.entities.map { Entity(EntityType(it.entityTypeName), it.role) })
                            }
                    },
                    { EntityType(it) }
                )
            }
            val modelHolder = StanfordModelBuilder.buildIntentModel(
                context,
                StanfordModelBuilder.defaultNlpApplicationConfiguration(), expressions
            )
            val classifier = StanfordIntentClassifier(modelHolder)
            val classification =
                classifier.classifyIntent(context, sentence, tokenizer.tokenize(TokenizerContext(context), sentence))

            classification.next()
            val p1 = classification.probability()
            classification.next()
            val p2 = classification.probability()
            assertFalse(classification.hasNext())
            assertTrue(p1 > p2)
            assertTrue(p1 > 0 && p1 < 1)
            assertTrue(p2 > 0 && p2 < 1)
        }
    }
}