package at.hannibal2.skyhanni.features.mining

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
object DonExpressoFeedingReminder {

    private val config get() = SkyHanniMod.feature.mining

    private val patternGroup = RepoPattern.group("mining.donexpresso")

    private val fullPattern by patternGroup.pattern(
        "full",
        "\\[NPC\\] Don Expresso: I DON'T FEEL SO GOOD\\.\\.\\."
    )

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        if (!fullPattern.matches(event.cleanMessage)) return

        ChatUtils.clickToActionOrDisable(
            "§6Don Expresso §eis full! Click to teleport to him.",
            config::donExpressoFeedingReminder,
            actionName = "teleport to Don Expresso",
            action = { HypixelCommands.tpToDonExpresso() },
        )
    }

    fun isEnabled() = IslandType.DWARVEN_MINES.isInIsland() && config.donExpressoFeedingReminder
}
