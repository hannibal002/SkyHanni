package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.LorenzUtils

object TestBingo {
    var testBingo = false

    fun toggle() {
        testBingo = !testBingo
        LorenzUtils.chat("§e[SkyHanni] Test Bingo " + (if (testBingo) "enabled" else "disabled"))
    }
}