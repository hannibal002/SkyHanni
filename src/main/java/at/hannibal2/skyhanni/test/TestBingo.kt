package at.hannibal2.hanni.test

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.events.DebugDataCollectEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils

@HanniModule
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
