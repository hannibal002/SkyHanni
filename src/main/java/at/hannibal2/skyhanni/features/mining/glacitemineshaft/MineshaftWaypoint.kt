package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.corpse.CorpseType
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec

data class MineshaftWaypoint(
    var type: Type,
    val location: LorenzVec,
    var corpseType: CorpseType? = null,
    var isShared: Boolean = false,
) {
    val isCorpse get() = corpseType != null
    val label get() = type.label(this)
    val labelScale get() = type.labelScale
    val textDisplay get() = type.labelColor.getChatColor() + label
    val fillColor get() = type.fillColor(this)
    val fillMaxAlpha get() = type.fillMaxAlpha
    val shouldRender get() = type.renderCondition(this)

    enum class Type(
        val label: (MineshaftWaypoint) -> String,
        val labelColor: LorenzColor,
        val labelScale: Double = 1.0,
        val fillColor: (MineshaftWaypoint) -> LorenzColor,
        val fillMaxAlpha: Float = 0.33f,
        val renderCondition: (MineshaftWaypoint) -> Boolean,
    ) {
        ENTRANCE(
            label = { "Entrance" },
            labelColor = LorenzColor.YELLOW,
            fillColor = { LorenzColor.YELLOW },
            renderCondition = { config.types.entrance },
        ),
        LADDER(
            label = { "Ladder" },
            labelColor = LorenzColor.YELLOW,
            fillColor = { LorenzColor.YELLOW },
            renderCondition = { config.types.ladder },
        ),
        POTENTIAL_CORPSE(
            label = { "Potential Corpse" },
            labelColor = LorenzColor.AQUA,
            labelScale = 0.6,
            fillColor = { LorenzColor.AQUA },
            fillMaxAlpha = 0.2f,
            renderCondition = { config.types.potentialCorpse },
        ),
        FOUND_CORPSE(
            label = { "${it.corpseType?.type ?: "Unknown"} Corpse" },
            labelColor = LorenzColor.YELLOW,
            fillColor = { it.corpseType?.color ?: LorenzColor.DARK_GRAY },
            renderCondition = { config.types.foundCorpse },
        ),
        LOOTED_CORPSE(
            label = { "${it.corpseType?.type ?: "Unknown"} Corpse" },
            labelColor = LorenzColor.GREEN,
            fillColor = { it.corpseType?.color ?: LorenzColor.DARK_GRAY },
            renderCondition = { config.types.lootedCorpse },
        ),
        ;

        companion object {
            private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft.waypointsConfig
        }
    }
}
