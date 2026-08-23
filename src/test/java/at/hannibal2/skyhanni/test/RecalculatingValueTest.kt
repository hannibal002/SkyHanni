package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.TimeProvider
import at.hannibal2.skyhanni.utils.StableOrTransientValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RecalculatingValueTest {

    private var currentTime = 0L

    @BeforeEach
    fun setUp() {
        currentTime = 0L
        SimpleTimeMark.timeProvider = TimeProvider { currentTime }
    }

    @AfterEach
    fun tearDown() {
        SimpleTimeMark.resetTimeProvider()
    }

    @Test
    fun `stable real values do not expire`() {
        var calculations = 0
        val value = StableOrTransientValue(1.seconds) {
            calculations++
            StableOrTransientValue.stable("real-$calculations")
        }

        assertEquals("real-1", value.get())
        advanceTime(1_001.milliseconds)
        assertEquals("real-1", value.get())
        assertEquals(1, calculations)
    }

    @Test
    fun `transient null values recalculate after expiry`() {
        var calculations = 0
        val value = StableOrTransientValue<String?>(1.seconds) {
            calculations++
            StableOrTransientValue.transient(null)
        }

        assertEquals(null, value.get())
        advanceTime(1_001.milliseconds)
        assertEquals(null, value.get())
        assertEquals(2, calculations)
    }

    @Test
    fun `transient fallback values recalculate after expiry`() {
        var calculations = 0
        var realValueAvailable = false
        val value = StableOrTransientValue(1.seconds) {
            calculations++
            if (realValueAvailable) StableOrTransientValue.stable("real")
            else StableOrTransientValue.transient("fallback")
        }

        assertEquals("fallback", value.get())
        realValueAvailable = true
        advanceTime(1_001.milliseconds)
        assertEquals("real", value.get())
        assertEquals(2, calculations)
    }

    @Test
    fun `reset invalidates stable real values`() {
        var calculatedValue = "real-1"
        val value = StableOrTransientValue(1.seconds) {
            StableOrTransientValue.stable(calculatedValue)
        }

        assertEquals("real-1", value.get())
        calculatedValue = "real-2"
        value.reset()
        assertEquals("real-2", value.get())
    }

    private fun advanceTime(duration: Duration) {
        currentTime += duration.inWholeMilliseconds
    }
}
