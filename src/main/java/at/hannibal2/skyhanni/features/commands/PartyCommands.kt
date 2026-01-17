package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.PartyApi.partyLeader
import at.hannibal2.skyhanni.data.PartyApi.transferVoluntaryPattern
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.misc.limbo.LimboTimeTracker
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpace

@SkyHanniModule
object PartyCommands {

    private val config get() = SkyHanniMod.feature.misc.commands

    private fun kickOffline() {
        if (!config.shortCommands) return
        HypixelCommands.partyKickOffline()
    }

    private fun disband() {
        if (!config.shortCommands) return
        HypixelCommands.partyDisband()
    }

    private fun warp() {
        if (!config.shortCommands) return
        HypixelCommands.partyWarp()
    }

    private fun kick(kickedPlayer: String, kickedReason: String? = null) {
        if (!config.shortCommands) return
        if (kickedReason != null && config.partyKickReason) {
            HypixelCommands.partyChat("Kicking $kickedPlayer: $kickedReason")
        }
        HypixelCommands.partyKick(kickedPlayer)
    }

    private fun transfer(name: String? = null) {
        if (name == null) {
            if (LimboTimeTracker.inLimbo) {
                LimboTimeTracker.printStats(true)
                return
            }
            HypixelCommands.playtime()
            return
        }
        if (!config.shortCommands) return
        HypixelCommands.partyTransfer(name)
    }

    private fun promote(name: String) {
        if (!config.shortCommands) return
        HypixelCommands.partyPromote(name)
    }

    private fun reverseTransfer() {
        if (!config.reversePT.command) return
        val prevPartyLeader = PartyApi.prevPartyLeader ?: return

        autoPartyTransfer(prevPartyLeader)
    }

    private fun autoPartyTransfer(prevPartyLeader: String) {
        HypixelCommands.partyTransfer(prevPartyLeader)
        config.reversePT.message.takeIf { it.isNotBlank() }?.let {
            HypixelCommands.partyChat(it)
        }
    }

    @HandleEvent
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!config.partyKickReason) {
            return
        }
        if (!event.message.startsWith("/party kick ", ignoreCase = true) && !event.message.startsWith("/p kick ", ignoreCase = true)) {
            return
        }
        val args = event.message.substringAfter("kick").trim().split(" ")
        if (args.isEmpty()) return
        val kickedPlayer = args[0]
        val kickReason = args.drop(1).joinToString(" ").trim()
        if (kickReason.isEmpty()) return
        event.cancel()
        HypixelCommands.partyChat("Kicking $kickedPlayer: $kickReason")
        HypixelCommands.partyKick(kickedPlayer)
    }

    fun customTabComplete(command: String): List<String>? {
        if (command == "pk" || command == "pt" || command == "pp" && config.shortCommands) {
            return PartyApi.partyMembers
        }
        return null
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(5, "commands.usePartyTransferAlias", "commands.shortCommands")

        event.move(31, "commands", "misc.commands")
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onChat(event: SkyHanniChatEvent) {
        if (!config.reversePT.clickable) return

        // The message was likely already modified by us, return to avoid infinite recursion
        if (event.chatComponent.style.clickEvent != null) return

        if (!transferVoluntaryPattern.matches(event.message.trimWhiteSpace().removeColor())) return
        if (partyLeader != PlayerUtils.getName()) return

        val prevPartyLeader = PartyApi.prevPartyLeader ?: return
        event.blockedReason = "replacing"

        ChatUtils.clickableChat(
            event.message,
            onClick = { autoPartyTransfer(prevPartyLeader) },
            prefix = false,
        )
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("pko") {
            description = "Kicks offline party members"
            category = CommandCategory.SHORTENED_COMMANDS
            simpleCallback { kickOffline() }
        }
        event.registerBrigadier("pw") {
            description = "Warps your party"
            category = CommandCategory.SHORTENED_COMMANDS
            simpleCallback { warp() }
        }
        event.registerBrigadier("pk") {
            description = "Kick a specific party member"
            category = CommandCategory.SHORTENED_COMMANDS
            arg("name", BrigadierArguments.string()) { name ->
                argCallback("reason", BrigadierArguments.greedyString()) { reason ->
                    kick(getArg(name), reason)
                }
                callback {
                    kick(getArg(name))
                }
            }
        }
        event.registerBrigadier("pt") {
            description = "Transfer the party to another party member"
            category = CommandCategory.SHORTENED_COMMANDS
            argCallback("name", BrigadierArguments.string()) { name ->
                transfer(name)
            }
            simpleCallback {
                transfer()
            }
        }
        event.registerBrigadier("pp") {
            description = "Promote a specific party member"
            category = CommandCategory.SHORTENED_COMMANDS
            argCallback("name", BrigadierArguments.string()) { name ->
                promote(name)
            }
        }
        event.registerBrigadier("pd") {
            description = "Disbands the party"
            category = CommandCategory.SHORTENED_COMMANDS
            simpleCallback { disband() }
        }
        event.registerBrigadier("rpt") {
            description = "Reverse transfer party to the previous leader"
            category = CommandCategory.SHORTENED_COMMANDS
            simpleCallback { reverseTransfer() }
        }
    }
}
