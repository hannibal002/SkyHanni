package at.hannibal2.skyhanni.test.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.EnforcedConfigValues
import at.hannibal2.skyhanni.config.storage.EnforcedUserValuesStorage
import at.hannibal2.skyhanni.data.ElectionCandidate
import at.hannibal2.skyhanni.data.jsonobjects.repo.EnforcedValue
import at.hannibal2.skyhanni.data.jsonobjects.repo.EnforcedValueData
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNull

class EnforcedConfigValuesTest {

    companion object {
        private val config get() = SkyHanniMod.feature.dev.debug

        private var enabled
            get() = config.enabled
            set(value) {
                config.enabled = value
            }

        private var assumeMayor
            get() = config.assumeMayor.get()
            set(value) {
                config.assumeMayor.set(value)
            }

        val userValues get() = EnforcedConfigValues.userValues

        private const val ENABLED = "dev.debug.enabled"
        private const val ASSUME_MAYOR = "dev.debug.assumeMayor"

        @BeforeAll
        @JvmStatic
        fun setup() {
            SkyHanniMod.enforcedUserValuesStorage = EnforcedUserValuesStorage()
            SkyHanniMod.configManager = mockk<ConfigManager>(relaxed = true)
        }
    }

    @BeforeEach
    fun cleanup() {
        enabled = true
        assumeMayor = DIANA
        userValues.clear()
        update()
    }

    @Test
    fun `enforced value overrides config and restores original`() {
        update(
            enforcedValue(ENABLED, false),
        )

        assertFalse(enabled)
        assertTrue(isEnforced(ENABLED))
        assertEquals(JsonPrimitive(true), userValues[ENABLED])

        update()

        assertTrue(enabled)
        assertFalse(isEnforced(ENABLED))
        assertFalse(userValues.containsKey(ENABLED))
    }

    @Test
    fun `persisted value remains enforced value without backup`() {
        update(
            enforcedValue(ENABLED, false, persist = true),
        )

        assertFalse(enabled)
        assertFalse(userValues.containsKey(ENABLED))

        update()

        assertFalse(enabled)
        assertFalse(isEnforced(ENABLED))
    }

    @Test
    fun `backup is created only once while value is enforced`() {
        update(
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.PAUL),
        )

        // Simulate the user/config changing the value while it is enforced.
        assumeMayor = AATROX

        update(
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.PAUL),
        )

        assertEquals(ElectionCandidate.PAUL, assumeMayor)
        assertTrue(isEnforced(ASSUME_MAYOR))
        assertEquals(JsonPrimitive(ElectionCandidate.DIANA.name), userValues[ASSUME_MAYOR])

        update()

        // Restore the value from before enforcement, not the intermediate value.
        assertEquals(ElectionCandidate.DIANA, assumeMayor)
    }

    @Test
    fun `changing enforced value keeps original backup`() {
        update(
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.PAUL),
        )

        update(
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.AATROX),
        )

        assertEquals(ElectionCandidate.AATROX, assumeMayor)

        update()

        // The backup was made when enforcement started.
        assertEquals(ElectionCandidate.DIANA, assumeMayor)
    }

    @Test
    fun `multiple values are enforced and restored independently`() {
        update(
            enforcedValue(ENABLED, false),
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.PAUL),
        )

        assertFalse(enabled)
        assertEquals(ElectionCandidate.PAUL, assumeMayor)
        assertTrue(isEnforced(ENABLED))
        assertTrue(isEnforced(ASSUME_MAYOR))

        update(
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.PAUL),
        )

        // Only the expired enforcement is restored.
        assertTrue(enabled)
        assertEquals(ElectionCandidate.PAUL, assumeMayor)
        assertFalse(isEnforced(ENABLED))
        assertTrue(isEnforced(ASSUME_MAYOR))
    }

    @Test
    fun `persisted and non-persisted values are independent`() {
        update(
            enforcedValue(ENABLED, false, persist = true),
            enforcedValue(ASSUME_MAYOR, ElectionCandidate.PAUL),
        )

        assertFalse(enabled)
        assertEquals(ElectionCandidate.PAUL, assumeMayor)
        assertFalse(userValues.containsKey(ENABLED))
        assertTrue(userValues.containsKey(ASSUME_MAYOR))

        update()

        assertFalse(enabled)
        assertEquals(ElectionCandidate.DIANA, assumeMayor)
    }

    @Test
    fun `enforcement metadata is exposed`() {
        update(
            enforcedValue(
                path = ENABLED,
                value = false,
                extraMessage = "This setting is managed by the server.",
            ),
        )

        assertEquals(
            "This setting is managed by the server.",
            enforcementMessage(ENABLED),
        )

        update()

        assertNull(enforcementMessage(ENABLED))
    }

    @Test
    fun `unknown entries are ignored`() {
        assertFalse(isEnforced("dev.debug.doesNotExist"))
        userValues["dev.debug.doesNotExist"] = JsonPrimitive(false)
        assertDoesNotThrow { update() }
    }

    @Test
    fun `invalid entries for type are ignored`() {
        // Gson converts any random string to false without throwing an exception,
        // so don't try and test ENABLED with a string value.
        update(
            enforcedValue(ASSUME_MAYOR, false),
        )

        // The enforced value is not applied because it's of the wrong type.
        assertEquals(ElectionCandidate.DIANA, assumeMayor)
        // This is not required behavior, if this ever changes just update the test to reflect the new behavior.
        assertTrue(isEnforced(ASSUME_MAYOR))
    }

    @Test fun `left over user values are restored`() {
        userValues[ENABLED] = JsonPrimitive(false)
        update()
        assertFalse(enabled)
        assertFalse(userValues.containsKey(ENABLED))
    }

    private fun enforcedValue(
        path: String,
        value: JsonElement,
        extraMessage: String? = null,
        persist: Boolean = false,
    ) = EnforcedValueData(
        enforcedValues = listOf(
            EnforcedValue(
                path = path,
                value = value,
                persist = persist,
            ),
        ),
        affectedVersion = SkyHanniMod.modVersion,
        extraMessage = extraMessage,
    )

    private fun enforcedValue(
        path: String,
        value: Boolean,
        extraMessage: String? = null,
        persist: Boolean = false,
    ) = enforcedValue(
        path,
        JsonPrimitive(value),
        extraMessage,
        persist,
    )

    private fun enforcedValue(
        path: String,
        value: Enum<*>,
        extraMessage: String? = null,
        persist: Boolean = false,
    ) = enforcedValue(
        path,
        JsonPrimitive(value.name),
        extraMessage,
        persist,
    )

    private fun update(vararg values: EnforcedValueData) {
        EnforcedConfigValues.updateData(values.toList())
    }

    private fun isEnforced(path: String): Boolean = enforcementMessage(path) != null

    private fun enforcementMessage(path: String): String? =
        EnforcedConfigValues.isBlockedFromEditing(path)
}
