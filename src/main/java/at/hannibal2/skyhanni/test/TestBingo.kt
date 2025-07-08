package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils

@SkyHanniModule
object TestBingo {

    var testBingo = false

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Bingo Test")
        if (testBingo) {
            event.addData {
                add("debug active!")
            }
        } else {
            event.addIrrelevant("not active.")
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestbingo") {
            description = "Toggle the test bingo card display mode"
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback {
                testBingo = !testBingo
                ChatUtils.chat("Test Bingo " + (if (testBingo) "enabled" else "disabled"))
            }
        }
    }
}
