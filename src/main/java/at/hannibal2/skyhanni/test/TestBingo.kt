package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.ChatUtils

object TestBingo {

    var testBingo = false

    fun toggle() {
        testBingo = !testBingo
        ChatUtils.chat("Test Bingo " + (if (testBingo) "enabled" else "disabled"))
    }
}
