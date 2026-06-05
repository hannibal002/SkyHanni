package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class TimeMarkTest {
    @Test
    fun `far past is not close to now`() {
        Assertions.assertTrue(ServerTimeMark.farPast().passedSince() > 30.days)
        Assertions.assertTrue(SimpleTimeMark.farPast().passedSince() > 30.days)
    }

    @Test
    fun `subtracting duration from far past does not underflow`() {
        Assertions.assertTrue(ServerTimeMark.farPast() - 30.days <= ServerTimeMark.farPast())
        Assertions.assertTrue(SimpleTimeMark.farPast() - 30.days <= SimpleTimeMark.farPast())
    }

    @Test
    fun `the future should be in the future`() {
        Assertions.assertTrue(ServerTimeMark.now() + 30.days < ServerTimeMark.farFuture())
        Assertions.assertTrue(SimpleTimeMark.now() + 30.days < SimpleTimeMark.farFuture())
    }
}
