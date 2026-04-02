package at.hannibal2.skyhanni.features.event.winter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object ReindrakeWarpHelper {

    private val config get() = SkyHanniMod.feature.event.winter

    private val patternGroup = RepoPattern.group("event.winter.reindrakewarphelper")

    /**
     * REGEX-TEST: WOAH! [VIP] Georeek summoned a Reindrake from the depths!
     * REGEX-TEST: WOAH! [MVP+] DulceLyncis summoned TWO Reindrakes from the depths!
     */
    private val spawnPattern by patternGroup.pattern(
        "spawn.message",
        "WOAH! .+ summoned (?:a Reindrake|TWO Reindrakes) from the depths!",
    )

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        if (!spawnPattern.matches(event.cleanMessage)) return
        ChatUtils.clickToActionOrDisable(
            "A Reindrake was detected. Click to warp to the Winter Island spawn!",
            config::reindrakeWarpHelper,
            actionName = "warp to winter island spawn",
            action = { HypixelCommands.warp("winter") }
        )
    }

    fun isEnabled() = IslandType.WINTER.isCurrent() && config.reindrakeWarpHelper
}
