package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.test.NEUDependent.test
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import org.junit.jupiter.api.Test

object NEUDependent {
    fun SkyBlockTime.test(): Boolean {
        return true
    }
}

class NEUDependencyTest {
    @Test
    fun `test that we can have extensions of NEU on object classes`() {
        assert(SkyBlockTime(0).test())
    }
}