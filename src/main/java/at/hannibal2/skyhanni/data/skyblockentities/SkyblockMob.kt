package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.utils.SkyblockMobUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand


open class SkyblockMob(baseEntity: Entity, armorStand: EntityArmorStand?) : SkyblockEntity(baseEntity, armorStand) {
    private val regexResult by lazy { armorStand?.name?.removeColor()?.let { SkyblockMobUtils.mobNameFilter.find(it) } }

    override val name = regexResult?.groupValues?.get(3) ?: run { SkyblockMobUtils.errorNameFinding(baseEntity.name) }
    val level by lazy { regexResult?.groupValues?.get(2) }
}
