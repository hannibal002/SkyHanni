package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.dungeon.DungeonEnterEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SoundUtils.createSound
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DungeonTrinityHelper {
    private val config get() = SkyHanniMod.feature.dungeon.trinityHelper
    private val dragonSound by lazy { createSound("mob.enderdragon.growl", 1f) }

    @HandleEvent
    fun onDungeonEnter(event: DungeonEnterEvent) {
        if (!config.enabled) return

        val puzzleCount = TabWidget.DUNGEON_PUZZLE.matchMatcherFirstLine { group("amount") }?.toIntOrNull() ?: 0

        // https://hypixel.net/threads/best-way-to-get-trinitys-number-instead-of-acquiring-actual-friends.5489159/
        if (puzzleCount == 5) {
            dragonSound.playSound()
            LorenzUtils.sendTitle("§dTrinity ?!?!", 5.seconds)
            if (config.sendPartyChat) HypixelCommands.partyChat("5 Puzzle dungeon, watch out for Trinity room")
        }
    }
}
