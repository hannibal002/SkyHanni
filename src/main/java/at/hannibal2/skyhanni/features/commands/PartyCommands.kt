package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.FriendAPI
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PartyCommands {

    private val config get() = SkyHanniMod.feature.commands

    fun kickOffline() {
        if (!config.shortCommands) return
        if (PartyAPI.partyMembers.isEmpty()) return
        ChatUtils.sendCommandToServer("party kickoffline")
    }

    fun disband() {
        if (!config.shortCommands) return
        if (PartyAPI.partyMembers.isEmpty()) return
        ChatUtils.sendCommandToServer("party disband")
    }

    fun warp() {
        if (!config.shortCommands) return
        if (PartyAPI.partyMembers.isEmpty()) return
        ChatUtils.sendCommandToServer("party warp")
    }

    fun kick(args: Array<String>) {
        if (!config.shortCommands) return
        if (PartyAPI.partyMembers.isEmpty()) return
        if (args.isEmpty()) return
        if (args.size > 1 && config.partyKickReason) {
            ChatUtils.sendCommandToServer("pc Kicking ${args[0]}: ${args.drop(1).joinToString(" ").trim()}")
        }
        ChatUtils.sendCommandToServer("party kick ${args[0]}")
    }

    fun transfer(args: Array<String>) {
        if (args.isEmpty()) {
            ChatUtils.sendCommandToServer("pt")
            return
        }
        if (!config.shortCommands) return
        if (PartyAPI.partyMembers.isEmpty()) return
        ChatUtils.sendCommandToServer("party transfer ${args[0]}")
    }

    fun promote(args: Array<String>) {
        if (!config.shortCommands) return
        if (PartyAPI.partyMembers.isEmpty()) return
        if (args.isEmpty()) return
        ChatUtils.sendCommandToServer("party promote ${args[0]}")
    }

    @SubscribeEvent
    fun onSendCommand(event: MessageSendToServerEvent) {
        if (!config.partyKickReason) {
            return
        }
        if (!event.message.startsWith("/party kick ", ignoreCase = true)
            && !event.message.startsWith("/p kick ", ignoreCase = true)
        ) {
            return
        }
        val args = event.message.split(" ")
        if (args.size < 3) return
        val kickedPlayer = args[2]
        val kickReason = args.drop(3).joinToString(" ").trim()
        if (kickReason.isEmpty()) return
        event.cancel()
        ChatUtils.sendCommandToServer("pc Kicking $kickedPlayer: $kickReason")
        ChatUtils.sendCommandToServer("p kick $kickedPlayer")
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
