package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.Mob
import at.hannibal2.skyhanni.data.MobData
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.rayIntersects
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer

object MobUtils {
    private const val defaultArmorStandName = "Armor Stand"

    // The corresponding ArmorStand for a mob has always the ID + 1 (with some exceptions)
    fun getArmorStand(entity: Entity, offset: Int = 1) = getNextEntity(entity, offset) as? EntityArmorStand

    fun getNextEntity(entity: Entity, offset: Int) = EntityUtils.getEntityByID(entity.entityId + offset)

    fun getArmorStandByRangeAll(entity: Entity, range: Double) =
        EntityUtils.getEntitiesNearby<EntityArmorStand>(entity.getLorenzVec(), range)

    fun getArmorStandByRange(entity: Entity, range: Double) =
        getArmorStandByRangeAll(entity, range).filter { entity.rotationYaw == it.rotationYaw }.firstOrNull()

    fun EntityArmorStand.isDefaultValue() = this.name == defaultArmorStandName


    class OwnerShip(val ownerName: String) {
        val ownerPlayer = MobData.players.firstOrNull { it.name == ownerName }
        override fun equals(other: Any?): Boolean {
            if (other is EntityPlayer) return ownerPlayer == other || ownerName == other.name
            if (other is String) return ownerName == other
            return false
        }

        override fun hashCode(): Int {
            return ownerName.hashCode()
        }
    }


    fun rayTraceForSkyblockMob(entity: Entity, distance: Double, partialTicks: Float, offset: LorenzVec = LorenzVec()) =
        rayTraceForSkyblockMob(entity, partialTicks, offset)?.takeIf {
            it.baseEntity.distanceTo(entity.getLorenzVec()) <= distance
        }

    fun rayTraceForSkyblockMobs(entity: Entity, distance: Double, partialTicks: Float, offset: LorenzVec = LorenzVec()) =
        rayTraceForSkyblockMobs(entity, partialTicks, offset)?.filter {
            it.baseEntity.distanceTo(entity.getLorenzVec()) <= distance
        }.takeIf { it?.isNotEmpty() ?: false }

    fun rayTraceForSkyblockMob(entity: Entity, partialTicks: Float, offset: LorenzVec = LorenzVec()) =
        rayTraceForSkyblockMobs(entity, partialTicks, offset)?.first()

    fun rayTraceForSkyblockMobs(entity: Entity, partialTicks: Float, offset: LorenzVec = LorenzVec()): List<Mob>? {
        val pos = entity.getPositionEyes(partialTicks).toLorenzVec().add(offset)
        val look = entity.getLook(partialTicks).toLorenzVec().normalize()
        val possibleEntities = MobData.skyblockMobs.entityList.filter {
            it !is EntityArmorStand && it.entityBoundingBox.rayIntersects(
                pos, look
            )
        }
        if (possibleEntities.isEmpty()) return null
        return possibleEntities.toList().sortedBy { it.distanceTo(pos) }.map {
            MobData.entityToMob[it]
                ?: throw IllegalStateException("Entity not found even though in entity is in mob Set")
        }
    }

}
