package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.event.EventListeners
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.mining.GlaciteMineshaftDetectEvent
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftDetection
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils

@SkyHanniModule
object SkyBlockIslandTest {

    var testIsland: IslandType? = null
        set(value) {
            field = value
            EventListeners.markEventCacheDirty()
        }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
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
            description = "Changes the SkyBlock island SkyHanni thinks you are on"
            category = CommandCategory.DEVELOPER_TEST

            literalCallback("reset") {
                testIsland?.let {
                    ChatUtils.chat("Disabled test island (was ${it.displayName})")
                    testIsland = null
                    return@literalCallback
                }
                ChatUtils.chat("Test island was not set.")
            }

            literal("mineshaft") {
                MineshaftDetection.MineshaftType.entries.forEach { mineshaftType ->
                    literalCallback(mineshaftType.name.lowercase()) {
                        testIsland = IslandType.MINESHAFT
                        ChatUtils.chat("Set test island to ${IslandType.MINESHAFT.displayName}")
                        GlaciteMineshaftDetectEvent(mineshaftType).post()
                    }
                }
                callback {
                    testIsland = IslandType.MINESHAFT
                    ChatUtils.chat("Set test island to ${IslandType.MINESHAFT.displayName}")
                    GlaciteMineshaftDetectEvent(MineshaftDetection.MineshaftType.TOPA_1).post()
                }
            }

            argCallback("island", EnumArgumentType.lowercase<IslandType>(isGreedy = true)) { islandType ->
                testIsland = islandType
                ChatUtils.chat("Set test island to ${islandType.displayName}")
            }
            simpleCallback { ChatUtils.userError("Usage: /shtestisland <island name>/reset") }
        }
    }
}
