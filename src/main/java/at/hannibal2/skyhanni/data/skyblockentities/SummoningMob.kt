package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.utils.SkyblockMobUtils
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand

class SummoningMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?, result: MatchResult) : SkyblockEntity(baseEntity, armorStand) {
    override val name = result.groupValues[2]

    val owner = SkyblockMobUtils.ownerShip(result.groupValues[1])
}
