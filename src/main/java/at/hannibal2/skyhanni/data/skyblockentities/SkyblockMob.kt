package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.utils.MobUtils
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand


abstract class SkyblockMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?, extraEntitiesList: MutableList<EntityLivingBase>?) : SummingOrSkyblockMob(
    baseEntity, armorStand, extraEntitiesList
) {

    val hologram1 by lazy { MobUtils.getArmorStand(baseEntity, 2) }
    val hologram2 by lazy { MobUtils.getArmorStand(baseEntity, 3) }

}
