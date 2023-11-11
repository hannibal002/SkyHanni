package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.utils.SkyblockMobUtils
import net.minecraft.entity.EntityLivingBase

class DisplayNPC(baseEntity: EntityLivingBase) : SkyblockEntity(baseEntity, SkyblockMobUtils.getArmorStand(baseEntity)) {
    override val name = armorStand?.name ?: ""
}
