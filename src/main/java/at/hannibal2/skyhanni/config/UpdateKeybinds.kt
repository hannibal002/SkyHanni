package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.NotificationManager
import at.hannibal2.skyhanni.data.SkyHanniNotification
import at.hannibal2.skyhanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.SkyHanniConfigSearchResetCommand
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.json.Shimmy
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object UpdateKeybinds {

    var keybinds: MutableSet<String> = mutableSetOf()

    private val logger = LorenzLogger("keybind_upgrader")

    private fun fixKeybinds() {
        for (keybind in keybinds) {
            resetKeybind(keybind)
        }
    }

    private var hasUpdated = false

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onTick(event: HypixelJoinEvent) {
        if (hasUpdated) return
        hasUpdated = true
        val config = SkyHanniMod.feature
        val lastMcVersion = config.lastMinecraftVersion ?: "1.8.9"
        val currentMcVersion = PlatformUtils.MC_VERSION
        config.lastMinecraftVersion = currentMcVersion
        if (!config.storage.hasPlayedBefore) {
            logger.log("User has never used skyhanni before!")
            return
        }
        if (lastMcVersion != "1.8.9") return
        fixKeybinds()
    }

    private fun resetKeybind(key: String) {
        SkyHanniConfigSearchResetCommand.resetCommand(arrayOf("reset", "config.$key"))
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetkeybinds") {
            category = CommandCategory.USERS_RESET
            description = "Resets all of your skyhanni keybinds"
            aliases = listOf("shkeybindreset")
            simpleCallback {
                for (keybind in keybinds) {
                    resetKeybind(keybind)
                }
                ChatUtils.chat("§aSuccessfully reset all SkyHanni Keybinds")
            }
        }
    }
}
