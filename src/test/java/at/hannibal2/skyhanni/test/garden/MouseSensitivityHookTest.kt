package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.features.garden.sensitivity.MouseSensitivityManager
import at.hannibal2.skyhanni.mixins.hooks.MouseSensitivityHook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MouseSensitivityHookTest {

    @Test
    fun `locked sensitivity maps to exact zero`() {
        try {
            MouseSensitivityManager.state = MouseSensitivityManager.SensitivityState.LOCKED

            assertEquals(0f, MouseSensitivityHook.remapSensitivity(1f))
        } finally {
            MouseSensitivityManager.state = MouseSensitivityManager.SensitivityState.UNCHANGED
        }
    }
}
