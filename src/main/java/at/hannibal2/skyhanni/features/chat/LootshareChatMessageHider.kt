package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object LootshareChatMessageHider {

    private val config get() = SkyHanniMod.feature.chat
    private val patternGroup = RepoPattern.group("chat.lootshare")

    /**
     * REGEX-TEST: §e§lLOOT SHARE §r§r§r§fYou received loot for assisting §r§7jiangshuai666§r§f!
     */
    private val lootsharePattern by patternGroup.pattern(
        "lootshare",
        "§e§lLOOT SHARE §r(?:§r)*§fYou received loot for assisting §r.*§f!",
    )

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        if (!shouldHide(event.message)) return

        event.blockedReason = "lootshare"
    }

    private fun shouldHide(message: String): Boolean {
        lootsharePattern.matchMatcher(message) {
            return true
        }
        return false
    }

    fun isEnabled() = config.hideLootshareMessages
}
