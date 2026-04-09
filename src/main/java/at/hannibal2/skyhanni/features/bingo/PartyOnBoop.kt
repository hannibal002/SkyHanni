package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PrivateMessageChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object PartyOnBoop {

    private val config get() = SkyHanniMod.feature.misc.boopParty
    private val patternGroup = RepoPattern.group("bingo")

    /**
     * REGEX-TEST: §d§lBoop!
     */
    private val boopPattern by patternGroup.pattern(
        "boop",
        "§d§lBoop!",
    )

    @HandleEvent
    fun onPrivateMessageChat(event: PrivateMessageChatEvent.Allow) {
        if (!isEnabled()) return
        val direction = event.direction ?: "From"
        ChatUtils.debug(direction)
        if (direction == "To") return
        val message = event.messageComponent.intoComponent().formattedTextCompat().removeResets()
        ChatUtils.debug("event.message = ${event.message} §r, message var. = $message")
        if (!boopPattern.matches(message)) return

        val username = event.author.cleanPlayerName(displayName = true)
        ChatUtils.clickableChat(
            "Click to invite $username §eto the party!",
            onClick = {
                HypixelCommands.partyInvite(username)
            },
        )
    }

    private fun isEnabled() = (SkyBlockUtils.isBingoProfile && config.boopPartyBingo) || config.boopParty
}
