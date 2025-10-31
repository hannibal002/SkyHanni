package at.hannibal2.hanni.features.event.winter

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object ReindrakeWarpHelper {

    private val config get() = HanniMod.feature.event.winter

    private val patternGroup = RepoPattern.group("event.winter.reindrakewarphelper")

    /**
     * REGEX-TEST: §c§lWOAH! §cA §4Reindrake §cwas summoned from the depths!
     * REGEX-TEST: §c§lWOAH! §r§cA §r§4Reindrake §r§cwas summoned from the depths!
     */
    private val spawnPattern by patternGroup.pattern(
        "spawn.message",
        "§c§lWOAH! (?:§r)?§cA (?:§r)?§4Reindrake (?:§r)?§cwas summoned from the depths!",
    )

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return
        if (!spawnPattern.matches(event.message)) return
        ChatUtils.clickToActionOrDisable(
            "A Reindrake was detected. Click to warp to the Winter Island spawn!",
            config::reindrakeWarpHelper,
            actionName = "warp to winter island spawn",
            action = { HypixelCommands.warp("winter") }
        )
    }

    fun isEnabled() = IslandType.WINTER.isCurrent() && config.reindrakeWarpHelper
}
