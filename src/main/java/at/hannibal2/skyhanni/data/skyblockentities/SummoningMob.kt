package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.utils.MobUtils
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand

class SummoningMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?, extraEntitiesList: MutableList<EntityLivingBase>?, result: MatchResult) : SummingOrSkyblockMob(baseEntity, armorStand, extraEntitiesList) {
    override val name = result.groupValues[2]

    val owner = MobUtils.OwnerShip(result.groupValues[1])
}
