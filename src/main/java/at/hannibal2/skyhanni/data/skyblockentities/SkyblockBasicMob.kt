package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.data.MobFilter
import at.hannibal2.skyhanni.utils.EntityUtils.isCorrupted
import at.hannibal2.skyhanni.utils.EntityUtils.isRunic
import at.hannibal2.skyhanni.utils.LorenzUtils.get
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand

class SkyblockBasicMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?, extraEntities: List<EntityLivingBase>) : SkyblockMob(
    baseEntity, armorStand, extraEntities.toMutableList()
) {
    private val regexResult = armorStand?.name?.removeColor()?.let { MobFilter.mobNameFilter.find(it) }

    private fun removeCorruptedSuffix(string: String) =
        if (regexResult[3]?.isNotEmpty() == true) string.dropLast(1) else string

    override val name = regexResult[4]?.let { removeCorruptedSuffix(it) } ?: run {
        MobFilter.errorNameFinding(baseEntity.name)
    }
    val level = regexResult[2]
    val isCorrupted get() = baseEntity.isCorrupted() // Can change
    val isRunic = baseEntity.isRunic() // Does not Change
}
