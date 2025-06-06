package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.features.dungeon.CroesusChestTracker
import at.hannibal2.skyhanni.features.dungeon.floor7.TerminalInfo
import at.hannibal2.skyhanni.features.mining.MineshaftPityDisplay
import at.hannibal2.skyhanni.features.minion.MinionFeatures
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
@Suppress("LargeClass", "LongMethod")
@Deprecated("do not use this class anymore")
object Commands {
    // Do not add new commands in this class
    // TODO move all command loading away from this class

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        usersNormalReset(event)
        usersBugFix(event)
    }

    private fun usersNormalReset(event: CommandRegistrationEvent) {
        // non trackers
        event.register("shresetkismet") {
            description = "Resets the saved values of the applied kismet feathers in Croesus"
            category = CommandCategory.USERS_RESET
            callback { CroesusChestTracker.resetChest() }
        }
        event.register("shresetmineshaftpitystats") {
            description = "Resets the mineshaft pity display stats"
            category = CommandCategory.USERS_RESET
            callback { MineshaftPityDisplay.fullResetCounter() }
        }
        event.register("shresetterminal") {
            description = "Resets terminal highlights in F7."
            category = CommandCategory.USERS_RESET
            callback { TerminalInfo.resetTerminals() }
        }
    }

    private fun usersBugFix(event: CommandRegistrationEvent) {
        event.register("shfixminions") {
            description = "Removed bugged minion locations from your private island"
            category = CommandCategory.USERS_BUG_FIX
            callback { MinionFeatures.removeBuggedMinions(isCommand = true) }
        }
    }
}
