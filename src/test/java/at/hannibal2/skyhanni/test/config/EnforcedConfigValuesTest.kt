package at.hannibal2.skyhanni.test.config

import at.hannibal2.skyhanni.config.EnforcedConfigValues
import at.hannibal2.skyhanni.data.jsonobjects.repo.EnforcedValue
import com.google.gson.JsonPrimitive
import io.github.notenoughupdates.moulconfig.observer.Property
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test

class EnforcedConfigValuesTest {
    private class Config { val enabled: Property<Boolean> = Property.of(true) }

    @AfterEach
    fun cleanup() = EnforcedConfigValues.userValues.clear()

    @Test
    fun `records the backup even when a property observer throws`() {
        val config = Config()
        config.enabled.whenChanged { _, _ -> error("observer failed") }

        assertThrows<IllegalStateException> {
            EnforcedConfigValues.enforceValue(config, EnforcedValue("enabled", JsonPrimitive(false)))
        }

        assertFalse(config.enabled.get())
        val backup = EnforcedConfigValues.userValues["enabled"]
        assertEquals(JsonPrimitive(true), backup?.userValue)
        assertEquals(JsonPrimitive(false), backup?.enforcedValue)
    }

    @Test
    fun `re-enforcing the same value keeps the backup without notifying observers`() {
        val config = Config()
        EnforcedConfigValues.enforceValue(config, EnforcedValue("enabled", JsonPrimitive(false)))
        var notifications = 0
        config.enabled.whenChanged { _, _ -> notifications++ }

        EnforcedConfigValues.enforceValue(config, EnforcedValue("enabled", JsonPrimitive(false)))

        assertEquals(0, notifications)
        assertEquals(JsonPrimitive(true), EnforcedConfigValues.userValues["enabled"]?.userValue)
    }
}
