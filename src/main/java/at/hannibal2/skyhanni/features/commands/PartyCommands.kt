package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.FriendAPI
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PartyCommands {
    private val config get() = SkyHanniMod.feature.commands
    fun kickOffline() {
        if (!config.shortCommands) return
        if (PartyAPI.partyMembers.isEmpty()) return
        LorenzUtils.sendCommandToServer("party kickoffline")
    }

    fun warp() {
        if (!config.shortCommands) return
        if (PartyAPI.partyMembers.isEmpty()) return
        LorenzUtils.sendCommandToServer("party warp")
    }

    fun kick(args: Array<String>) {
        if (!config.shortCommands) return
        if (PartyAPI.partyMembers.isEmpty()) return
        if (args.isEmpty()) return
        LorenzUtils.sendCommandToServer("party kick ${args[0]}")
    }

    fun transfer(args: Array<String>) {
        if (args.isEmpty()) LorenzUtils.sendCommandToServer("pt")
        if (!config.shortCommands) return
        if (PartyAPI.partyMembers.isEmpty()) return
        LorenzUtils.sendCommandToServer("party transfer ${args[0]}")
    }

    fun promote(args: Array<String>) {
        if (!config.shortCommands) return
        if (PartyAPI.partyMembers.isEmpty()) return
        if (args.isEmpty()) return
        LorenzUtils.sendCommandToServer("party promote ${args[0]}")
    }

    fun customTabComplete(command: String): List<String>? {
        if (command == "pk" || command == "pt" || command == "pp" && config.shortCommands) {
            return PartyAPI.partyMembers
        }

        if (command == "p" || command == "party") {
            val friends = if (config.tabComplete.friends) {
                FriendAPI.getAllFriends().filter { it.bestFriend || config.tabComplete.onlyBestFriends }.map { it.name }
            } else {
                emptyList<String>()
            }
            return friends + getPartyCommands()
        }
        return null
    }

    private fun getPartyCommands(): List<String> {
        return if (config.tabComplete.partyCommands && PartyAPI.partyMembers.isNotEmpty()) {
            otherPartyCommands
        } else emptyList()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(5, "commands.usePartyTransferAlias", "commands.shortCommands")
    }
}

private val otherPartyCommands = listOf(
    "Disband",
    "KickOffline",
    "Leave",
    "List",
    "Mute",
    "Private",
    "Warp",
    "Settings"
)