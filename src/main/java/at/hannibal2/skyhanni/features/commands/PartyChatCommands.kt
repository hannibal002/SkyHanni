package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.misc.PartyCommandsConfig
import at.hannibal2.skyhanni.data.FriendAPI
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.events.PartyChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PartyChatCommands {

    data class PartyChatCommand(
        val names: List<String>,
        val isEnabled: () -> Boolean,
        val requiresPartyLead: Boolean,
        val executable: (PartyChatEvent) -> Unit,
    )

    private fun useConfig() = SkyHanniMod.feature.misc.partyCommands
    val allPartyCommands = listOf(
        PartyChatCommand(
            listOf("pt", "ptme", "transfer"),
            { useConfig().transferCommand },
            requiresPartyLead = true,
            executable = {
                ChatUtils.sendCommandToServer("party transfer ${it.author}")
            }
        ),
        PartyChatCommand(
            listOf("pw", "warp", "warpus"),
            { useConfig().warpCommand },
            requiresPartyLead = true,
            executable = {
                ChatUtils.sendCommandToServer("party warp")
            }
        ),
    )

    val indexedPartyChatCommands = buildMap {
        for (command in allPartyCommands) {
            for (name in command.names) {
                put(name.lowercase(), command)
            }
        }
    }

    fun isTrustedUser(name: String): Boolean {
        val friend = FriendAPI.getAllFriends().find { it.name == name }
        return when (useConfig().defaultRequiredTrustLevel) {
            PartyCommandsConfig.TrustedUser.FRIENDS -> friend != null
            PartyCommandsConfig.TrustedUser.BEST_FRIENDS -> friend?.bestFriend == true
            PartyCommandsConfig.TrustedUser.ANYONE -> true
            PartyCommandsConfig.TrustedUser.NO_ONE -> false
        }
    }

    private val commandBeginChars = ".!?".toSet()

    @SubscribeEvent
    fun onPartyCommand(event: PartyChatEvent) {
        if (event.text.firstOrNull() !in commandBeginChars)
            return
        val commandLabel = event.text.substring(1).substringBefore(' ')
        val command = indexedPartyChatCommands[commandLabel.lowercase()] ?: return
        if (event.author == LorenzUtils.getPlayerName()) {
            return
        }
        if (!command.isEnabled()) return
        if (command.requiresPartyLead && PartyAPI.partyLeader != LorenzUtils.getPlayerName()) {
            return
        }
        if (!isTrustedUser(event.author)) {
            ChatUtils.chat("§cIgnoring chat command from ${event.author}. Change your party chat command settings or /friend (best) them.")
            return
        }
        command.executable(event)
    }
}
