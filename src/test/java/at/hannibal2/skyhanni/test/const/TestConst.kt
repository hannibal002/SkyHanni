package at.hannibal2.skyhanni.test.const

import at.hannibal2.skyhanni.utils.const.Const
import at.hannibal2.skyhanni.utils.const.liftConst
import at.hannibal2.skyhanni.utils.const.unconst
import org.junit.jupiter.api.Test

class TestConst {
    @Test
    fun testConstListLayout() {
        val list = listOf(Const.newUnchecked(""))
        val liftedList = list.liftConst()
        require(liftedList.unsafeMap { it[0] }.unconst == "")
    }
}
