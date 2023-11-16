package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.utils.SkyblockMobUtils.getArmorStandByRangeAll
import at.hannibal2.skyhanni.utils.SkyblockMobUtils.isDefaultValue
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.EntityLivingBase

class DisplayNPC(baseEntity: EntityLivingBase) : SkyblockEntity(baseEntity, getArmorStandByRangeAll(baseEntity, 1.0).firstOrNull { !it.name.startsWith("§e§lCLICK") && !it.isDefaultValue() }) {
    override val name = armorStand?.name?.removeColor() ?: ""
}
