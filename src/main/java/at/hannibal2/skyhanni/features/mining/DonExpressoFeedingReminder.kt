package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object DonExpressoFeedingReminder {

    private val config get() = SkyHanniMod.feature.mining

    private val patternGroup = RepoPattern.group("mining.donexpresso")

    /**
     * REGEX-TEST: [NPC] Don Expresso: I DON'T FEEL SO GOOD...
     */
    private val fullPattern by patternGroup.pattern(
        "full",
        "\\[NPC] Don Expresso: I DON'T FEEL SO GOOD\\.\\.\\.",
    )

    private val TASTY_MITHRIL = "MITHRIL_GOURMAND".toInternalName()

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.donExpressoFeedingReminder) return
        if (!fullPattern.matches(event.cleanMessage)) return
        if (!InventoryUtils.isItemInInventory(TASTY_MITHRIL)) return

        ChatUtils.clickToActionOrDisable(
            "§6Don Expresso §eis full! Click to teleport to him.",
            config::donExpressoFeedingReminder,
            actionName = "teleport to Don Expresso",
            action = { HypixelCommands.tpToDonExpresso() },
        )
    }
}
