package at.hannibal2.hanni.features.bingo

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.hypixel.chat.event.PrivateMessageChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.hanni.utils.StringUtils.removeResets
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object BingoBoopParty {

    private val config get() = HanniMod.feature.event.bingo.boopParty
    private val patternGroup = RepoPattern.group("bingo")

    /**
     * REGEX-TEST: §dFrom §b[MVP§3+§b] Tryp0MC§7: §d§lBoop!
     * REGEX-TEST: §dFrom §b[MVP§5+§b] martimavocado§7: §d§lBoop!
     */
    private val boopPattern by patternGroup.pattern(
        "boop",
        "§dFrom.*§d§lBoop!",
    )

    @HandleEvent
    fun onPrivateMessageChat(event: PrivateMessageChatEvent) {
        if (!isEnabled()) return
        val message = event.messageComponent.textComponent.formattedText.removeResets()
        if (!boopPattern.matches(message)) return

        val username = event.author.cleanPlayerName(displayName = true)
        ChatUtils.clickableChat(
            "Click to invite $username §eto the party!",
            onClick = {
                HypixelCommands.partyInvite(username)
            },
        )
    }

    private fun isEnabled() = SkyBlockUtils.isBingoProfile && config
}
