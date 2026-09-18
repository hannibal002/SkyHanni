package at.hannibal2.skyhanni.features.combat.end.dragon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.EndBoss
import at.hannibal2.skyhanni.events.EndBossFightEndEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo

/**
 * Dragon weight of a finished fight: printed to chat when the dragon dies and read by
 * [DragonLootDetection]. All input data comes from [DragonFightAPI] and [DragonFightState].
 */
@SkyHanniModule
object DragonWeight {

    private val config get() = SkyHanniMod.feature.combat.endIsland.dragon

    /** Weight of the last finished fight, read by [DragonLootDetection]. */
    var weight = 0.0
        private set

    private fun placementWeight(place: Int) = when (place) {
        -1 -> 10
        1 -> 200
        2 -> 175
        3 -> 150
        4 -> 125
        5 -> 110
        6, 7, 8 -> 100
        9, 10 -> 90
        11, 12 -> 80
        else -> 70
    }

    fun calculateWeight(eyes: Int, place: Int, topDamage: Double, yourDamage: Double): Double =
        placementWeight(if (yourDamage == 0.0) -1 else place) +
            100 * eyes +
            100 * (yourDamage / (topDamage.takeIf { it != 0.0 } ?: 1.0))

    @HandleEvent
    private fun onEndBossFightEnd(event: EndBossFightEndEvent) {
        if (event.boss != EndBoss.DRAGON) return
        weight = calculateWeight(DragonFightState.eyesPlaced, event.place, event.topDamage, event.yourDamage)
        if (!config.chat) return
        // Sent without the SkyHanni prefix so it blends into Hypixel's centered fight summary.
        ChatUtils.chat(
            "§f${" ".repeat(30)}§r§eYour Weight: §r§a${weight.roundTo(0).addSeparators()}",
            prefix = false,
        )
    }
}
