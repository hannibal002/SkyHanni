package at.hannibal2.hanni.features.dungeon

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.model.TabWidget
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.dungeon.DungeonStartEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.HypixelCommands

@HanniModule
object DungeonTrinityHelper {
    private val config get() = HanniMod.feature.dungeon.trinityHelper

    @HandleEvent
    fun onDungeonStart(event: DungeonStartEvent) {
        if (!config.enabled) return

        TabWidget.DUNGEON_PUZZLE.matchMatcherFirstLine {
            // https://hypixel.net/threads/best-way-to-get-trinitys-number-instead-of-acquiring-actual-friends.5489159/
            group("amount")?.toIntOrNull()?.takeIf { it >= 5 } ?: return@matchMatcherFirstLine

            TitleManager.sendTitle("§dTrinity ?!?!")
            if (config.sendPartyChat) HypixelCommands.partyChat("5 Puzzle dungeon, watch out for Trinity room")
        }
    }
}
