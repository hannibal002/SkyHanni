package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.getPlayerNameFromChatMessage
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpaceAndResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object BoopParty {

    val config get() = SkyHanniMod.feature.event.bingo.boopParty
    val patternGroup = RepoPattern.group("misc")
    private val boopPattern by patternGroup.pattern(
        "boop",
        "§dFrom.*§d§lBoop!"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        val msg = event.message.trimWhiteSpaceAndResets()
        boopPattern.matchMatcher(msg) {
            val username = msg.getPlayerNameFromChatMessage() ?: return
            ChatUtils.clickableChat("Click to invite $username to the party", onClick = {
                HypixelCommands.partyInvite(username)
            })
        }
    }

    fun isEnabled() = config && LorenzUtils.isBingoProfile
}
