package at.hannibal2.skyhanni.features.mining.glacitemineshaft.corpse

import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftWaypointType
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand

enum class CorpseType(
    val type: String,
    val color: LorenzColor,
    val waypointType: MineshaftWaypointType,
    helmetInternalName: String,
    keyInternalName: String? = null,
) {
    LAPIS("Lapis", LorenzColor.BLUE, MineshaftWaypointType.LAPIS, "LAPIS_ARMOR_HELMET"),
    TUNGSTEN("Tungsten", LorenzColor.GRAY, MineshaftWaypointType.TUNGSTEN, "MINERAL_HELMET", "TUNGSTEN_KEY"),
    UMBER("Umber", LorenzColor.GOLD, MineshaftWaypointType.UMBER, "ARMOR_OF_YOG_HELMET", "UMBER_KEY"),
    VANGUARD("Vanguard", LorenzColor.WHITE, MineshaftWaypointType.VANGUARD, "VANGUARD_HELMET", "SKELETON_KEY"),
    ;

    val displayName = color.getChatColor() + type
    val helmet = helmetInternalName.toInternalName()
    val key = keyInternalName?.toInternalName()

    override fun toString(): String = displayName

    companion object {
        fun fromEntityOrNull(entity: ArmorStand): CorpseType? {
            val helmetInternalName = entity.equipment.items[EquipmentSlot.HEAD]?.getInternalName() ?: return null

            return CorpseType.entries.firstOrNull { it.helmet == helmetInternalName }
        }

        fun isValidHelmet(internalName: NeuInternalName): Boolean {
            return CorpseType.entries.any { it.helmet == internalName }
        }
    }
}
