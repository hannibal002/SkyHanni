package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.utils.SkyblockMobUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand

// TODO Test Boss mobs
open class SkyblockMob(baseEntity: Entity, armorStand: EntityArmorStand?) : SkyblockEntity(baseEntity, armorStand) {
    override val name = armorStand?.name?.let {
        SkyblockMobUtils.mobNameFilter.find(it.removeColor())?.groupValues?.get(1) ?: it.removeColor()
    } ?: run { SkyblockMobUtils.errorNameFinding(baseEntity.name) }
}
