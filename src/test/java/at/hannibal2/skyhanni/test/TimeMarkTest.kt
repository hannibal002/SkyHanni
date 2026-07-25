package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days

class TimeMarkTest {
    // the minimum duration we work with (30 days)
    private val farThreshold = 30.days

    @Test
    fun `far past should not be close to now`() {
        Assertions.assertTrue(ServerTimeMark.farPast().passedSince() > farThreshold)
        Assertions.assertTrue(SimpleTimeMark.farPast().passedSince() > farThreshold)
    }

    @Test
    fun `far future should not be close to now`() {
        Assertions.assertTrue(ServerTimeMark.farFuture().timeUntil() > farThreshold)
        Assertions.assertTrue(SimpleTimeMark.farFuture().timeUntil() > farThreshold)
    }

    @Test
    fun `subtracting duration from far past does not underflow`() {
        Assertions.assertTrue(ServerTimeMark.farPast() - farThreshold <= ServerTimeMark.farPast())
        Assertions.assertTrue(SimpleTimeMark.farPast() - farThreshold <= SimpleTimeMark.farPast())
    }

    @Test
    fun `subtracting duration from far past is still far past`() {
        Assertions.assertTrue((SimpleTimeMark.farPast() - farThreshold).isFarPast())
        Assertions.assertTrue((ServerTimeMark.farPast() - farThreshold).isFarPast())
    }

    @Test
    fun `adding duration to far future does not overflow`() {
        Assertions.assertTrue(ServerTimeMark.farFuture() + farThreshold >= ServerTimeMark.farFuture())
        Assertions.assertTrue(SimpleTimeMark.farFuture() + farThreshold >= SimpleTimeMark.farFuture())
    }

    @Test
    fun `adding duration to far future is still far future`() {
        Assertions.assertTrue((SimpleTimeMark.farFuture() + farThreshold).isFarFuture())
        Assertions.assertTrue((ServerTimeMark.farFuture() + farThreshold).isFarFuture())
    }
}
