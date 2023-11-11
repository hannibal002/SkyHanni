package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand

class DisplayNPC(baseEntity: EntityLivingBase) : SkyblockEntity(baseEntity, null) {
    override val armorStand
        get() = EntityUtils.getEntitiesNearby<EntityArmorStand>(baseEntity.getLorenzVec(), 1.0)
            .firstOrNull { !it.name.startsWith("§e§lCLICK") }
    override val name get() = armorStand?.name ?: ""
}
