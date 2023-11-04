package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.data.EntityData
import at.hannibal2.skyhanni.utils.SkyblockMobUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand

class SummoningMob(baseEntity: Entity, armorStand: EntityArmorStand? = SkyblockMobUtils.getArmorStand(baseEntity), result: MatchResult) : SkyblockEntity(baseEntity, armorStand) {
    override val name = result.groupValues[1]

    private val ownerName = result.groupValues[0]
    val owner = EntityData.currentRealPlayers.firstOrNull { it.name == ownerName }
}
