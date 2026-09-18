package potentialbugs

import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NonStorageDoesntNeedExposeTest {
    @Test
    fun `reports Runnable buttons regardless of how their type is written or inferred`() {
        val findings = NonStorageDoesntNeedExpose(Config.empty).lintWithContext(
            createEnvironment(),
            """
                package at.hannibal2.skyhanni.config.features

                import java.lang.Runnable
                import java.lang.Runnable as ButtonAction

                annotation class Expose
                annotation class ConfigEditorButton
                typealias Action = java.lang.Runnable
                class StoredConfig

                fun createAction(): Runnable = Runnable {}

                class Config {
                    @Expose @ConfigEditorButton
                    val explicit: Runnable = Runnable {}
                    @Expose @ConfigEditorButton
                    val qualified: java.lang.Runnable = Runnable {}
                    @Expose @ConfigEditorButton
                    val inferred = Runnable {}
                    @Expose @ConfigEditorButton
                    val imported: ButtonAction = ButtonAction {}
                    @Expose @ConfigEditorButton
                    val aliased: Action = Runnable {}
                    @Expose @ConfigEditorButton
                    val factory = createAction()

                    @Expose @ConfigEditorButton
                    val stored = StoredConfig()
                    @Expose
                    val withoutButton = Runnable {}
                    @ConfigEditorButton
                    val withoutExpose = Runnable {}
                }
            """.trimIndent(),
        )

        assertEquals(
            listOf("explicit", "qualified", "inferred", "imported", "aliased", "factory")
                .map { "@Expose annotation is not needed on property $it" },
            findings.map { it.message },
        )
    }

    @Test
    fun `does not report a stored config type named Runnable`() {
        val findings = NonStorageDoesntNeedExpose(Config.empty).lintWithContext(
            createEnvironment(),
            """
                package at.hannibal2.skyhanni.config.storage

                annotation class Expose
                annotation class ConfigEditorButton
                class Runnable

                class Config {
                    @Expose @ConfigEditorButton
                    val stored: Runnable = Runnable()
                }
            """.trimIndent(),
        )

        assertEquals(emptyList<String>(), findings.map { it.message })
    }
}
