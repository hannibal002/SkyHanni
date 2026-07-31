package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.corpse.CorpseType
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec

data class MineshaftWaypoint(
    var type: Type,
    val location: LorenzVec,
    val corpseType: CorpseType? = null,
    var isShared: Boolean = false,
) {
    val isCorpse get() = corpseType != null

    enum class Type(
        val displayText: (MineshaftWaypoint) -> String,
        val displayTextColor: LorenzColor = LorenzColor.WHITE,
        val displayTextScale: Double = 1.0,
        val fillColor: (MineshaftWaypoint) -> LorenzColor,
        val renderCondition: () -> Boolean,
    ) {
        ENTRANCE(
            displayText = { "Entrance" },
            fillColor = { LorenzColor.YELLOW },
            renderCondition = { config.types.entrance },
        ),
        LADDER(
            displayText = { "Ladder" },
            fillColor = { LorenzColor.YELLOW },
            renderCondition = { config.types.ladder },
        ),
        FOUND_CORPSE(
            displayText = { "${it.corpseType?.type ?: "???"} Corpse" },
            displayTextColor = LorenzColor.YELLOW,
            fillColor = { it.corpseType?.color ?: LorenzColor.DARK_GRAY },
            renderCondition = { config.types.foundCorpse },
        ),
        LOOTED_CORPSE(
            displayText = { "${it.corpseType?.type ?: "???"} Corpse" },
            displayTextColor = LorenzColor.GREEN,
            fillColor = { it.corpseType?.color ?: LorenzColor.DARK_GRAY },
            renderCondition = { config.types.lootedCorpse },
        ),
        ;

        companion object {
            private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft.waypointsConfig
        }
    }
}
