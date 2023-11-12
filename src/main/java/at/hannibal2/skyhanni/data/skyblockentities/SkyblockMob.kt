package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.utils.EntityUtils.isCorrupted
import at.hannibal2.skyhanni.utils.EntityUtils.isRunic
import at.hannibal2.skyhanni.utils.LorenzUtils.get
import at.hannibal2.skyhanni.utils.SkyblockMobUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand


abstract class SkyblockMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?) : SkyblockEntity(baseEntity, armorStand) {
    val hologram1 by lazy { SkyblockMobUtils.getArmorStand(baseEntity, 2) }
    val hologram2 by lazy { SkyblockMobUtils.getArmorStand(baseEntity, 3) }
}

class SkyblockBasicMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?) : SkyblockMob(
    baseEntity, armorStand
) {
    private val regexResult = armorStand?.name?.removeColor()?.let { SkyblockMobUtils.mobNameFilter.find(it) }

    private fun removeCorruptedSuffix(string: String) =
        if (regexResult[3]?.isNotEmpty() == true) string.dropLast(1) else string

    override val name = regexResult[4]?.let { removeCorruptedSuffix(it) } ?: run {
        SkyblockMobUtils.errorNameFinding(baseEntity.name)
    }
    val level = regexResult[2]
    val isCorrupted get() = baseEntity.isCorrupted() // Can change
    val isRunic = baseEntity.isRunic() // Does not Change
}
