package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.dungeon.DungeonStartEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands

@SkyHanniModule
object DungeonTrinityHelper {
    private val config get() = SkyHanniMod.feature.dungeon.trinityHelper

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
