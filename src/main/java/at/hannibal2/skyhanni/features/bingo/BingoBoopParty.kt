package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PrivateMessageChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object BingoBoopParty {

    private val config get() = SkyHanniMod.feature.event.bingo.boopParty
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
    fun onChat(event: PrivateMessageChatEvent) {
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

    private fun isEnabled() = LorenzUtils.isBingoProfile && config
}
