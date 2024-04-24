package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.misc.PartyCommandsConfig
import at.hannibal2.skyhanni.data.FriendAPI
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.data.hypixel.chat.event.PartyChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object PartyChatCommands {

    private val config get() = SkyHanniMod.feature.misc.partyCommands

    data class PartyChatCommand(
        val names: List<String>,
        val isEnabled: () -> Boolean,
        val requiresPartyLead: Boolean,
        val executable: (PartyChatEvent) -> Unit,
    )

    private var lastWarp = SimpleTimeMark.farPast()
    private var lastAllInvite = SimpleTimeMark.farPast()

    private val allPartyCommands = listOf(
        PartyChatCommand(
            listOf("pt", "ptme", "transfer"),
            { config.transferCommand },
            requiresPartyLead = true,
            executable = {
                ChatUtils.sendCommandToServer("party transfer ${it.cleanedAuthor}")
            }
        ),
        PartyChatCommand(
            listOf("pw", "warp", "warpus"),
            { config.warpCommand && lastWarp.passedSince() > 5.seconds },
            requiresPartyLead = true,
            executable = {
                lastWarp = SimpleTimeMark.now()
                ChatUtils.sendCommandToServer("party warp")
            }
        ),
        PartyChatCommand(
            listOf("allinv", "allinvite"),
            { config.allInviteCommand && lastAllInvite.passedSince() > 2.seconds },
            requiresPartyLead = true,
            executable = {
                lastAllInvite = SimpleTimeMark.now()
                ChatUtils.sendCommandToServer("party settings allinvite")
            }
        ),
    )

    private val indexedPartyChatCommands = buildMap {
        for (command in allPartyCommands) {
            for (name in command.names) {
                put(name.lowercase(), command)
            }
        }
    }

    private fun isTrustedUser(name: String): Boolean {
        val friend = FriendAPI.getAllFriends().find { it.name == name }
        return when (config.defaultRequiredTrustLevel) {
            PartyCommandsConfig.TrustedUser.FRIENDS -> friend != null
            PartyCommandsConfig.TrustedUser.BEST_FRIENDS -> friend?.bestFriend == true
            PartyCommandsConfig.TrustedUser.ANYONE -> true
            PartyCommandsConfig.TrustedUser.NO_ONE -> false
        }
    }

    private val commandBeginChars = ".!?".toSet()

    @SubscribeEvent
    fun onPartyCommand(event: PartyChatEvent) {
        if (event.message.firstOrNull() !in commandBeginChars)
            return
        val commandLabel = event.message.substring(1).substringBefore(' ')
        val command = indexedPartyChatCommands[commandLabel.lowercase()] ?: return
        val name = event.cleanedAuthor
        if (name == LorenzUtils.getPlayerName()) {
            return
        }
        if (!command.isEnabled()) return
        if (command.requiresPartyLead && PartyAPI.partyLeader != LorenzUtils.getPlayerName()) {
            return
        }
        if (!isTrustedUser(name)) {
            ChatUtils.chat("§cIgnoring chat command from $name. Change your party chat command settings or /friend (best) them.")
            return
        }
        command.executable(event)
    }
}
