package at.hannibal2.skyhanni.features.mining.glacitemineshaft.corpse

import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftWaypointType
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

enum class CorpseType(
    val type: String,
    val color: LorenzColor,
    val waypointType: MineshaftWaypointType,
    helmet: String,
    key: String? = null,
) {
    LAPIS("Lapis", LorenzColor.BLUE, MineshaftWaypointType.LAPIS, "LAPIS_ARMOR_HELMET"),
    TUNGSTEN("Tungsten", LorenzColor.GRAY, MineshaftWaypointType.TUNGSTEN, "MINERAL_HELMET", "TUNGSTEN_KEY"),
    UMBER("Umber", LorenzColor.GOLD, MineshaftWaypointType.UMBER, "ARMOR_OF_YOG_HELMET", "UMBER_KEY"),
    VANGUARD("Vanguard", LorenzColor.WHITE, MineshaftWaypointType.VANGUARD, "VANGUARD_HELMET", "SKELETON_KEY"),
    ;

    val displayName = color.getChatColor() + type
    val helmet = helmet.toInternalName()
    val key = key?.toInternalName()

    override fun toString(): String = displayName

    companion object {
        fun getByHelmetOrNull(internalName: NeuInternalName): CorpseType? {
            return CorpseType.entries.firstOrNull { it.helmet == internalName }
        }
    }
}
