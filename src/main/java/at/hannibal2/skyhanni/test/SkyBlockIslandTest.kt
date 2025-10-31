package at.hannibal2.hanni.test

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.DebugDataCollectEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils

@HanniModule
object SkyBlockIslandTest {

    var testIsland: IslandType? = null

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Island Test")
        testIsland?.let {
            event.addData {
                add("debug active!")
                add("island: '$it'")
            }
        } ?: run {
            event.addIrrelevant("not active.")
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestisland") {
            description = "Changes the SkyBlock island Hanni thinks you are on"
            category = CommandCategory.DEVELOPER_TEST

            literal("reset") {
                callback {
                    testIsland?.let {
                        ChatUtils.chat("Disabled test island (was ${it.displayName})")
                        testIsland = null
                        return@callback
                    }
                    ChatUtils.chat("Test island was not set.")
                }
            }

            arg("island", BrigadierArguments.greedyString()) {
                callback {
                    val search = getArg(it).lowercase()
                    val found = find(search)
                    if (found == null) {
                        ChatUtils.userError("Unknown island type! ($search)")
                        return@callback
                    }
                    testIsland = found
                    ChatUtils.chat("Set test island to ${found.displayName}")

                }
            }
            callback { ChatUtils.userError("Usage: /shtestisland <island name>/reset") }
        }
    }

    private fun find(search: String): IslandType? {
        for (type in IslandType.entries) {
            if (type.name.equals(search, ignoreCase = true)) return type
            if (type.displayName.equals(search, ignoreCase = true)) return type
        }

        return null
    }
}
