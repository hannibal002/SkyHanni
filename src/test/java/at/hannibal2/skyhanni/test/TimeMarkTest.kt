package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class TimeMarkTest {
    @Test
    fun `far past should not be close to now`() {
        Assertions.assertTrue(ServerTimeMark.farPast().passedSince() > 30.days)
        Assertions.assertTrue(SimpleTimeMark.farPast().passedSince() > 30.days)
    }

    @Test
    fun `far future should not be close to now`() {
        Assertions.assertTrue(ServerTimeMark.farFuture().timeUntil() > 30.days)
        Assertions.assertTrue(ServerTimeMark.farFuture().timeUntil() > 30.days)
    }

    @Test
    fun `subtracting duration from far past does not underflow`() {
        Assertions.assertTrue(ServerTimeMark.farPast() - 30.days <= ServerTimeMark.farPast())
        Assertions.assertTrue(SimpleTimeMark.farPast() - 30.days <= SimpleTimeMark.farPast())
    }
}
