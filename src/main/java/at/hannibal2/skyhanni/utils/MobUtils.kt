package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.rayIntersects
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.compat.getInventoryItems
import net.minecraft.client.resources.language.I18n
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player

@SkyHanniModule
object MobUtils {

    private val defaultArmorStandName get() = I18n.get("entity.minecraft.armor_stand")

    // The corresponding ArmorStand for a mob has always the ID + 1 (with some exceptions)
    fun getArmorStand(entity: Entity, offset: Int = 1) = getNextEntity(entity, offset) as? ArmorStand

    fun getNextEntity(entity: Entity, offset: Int): Entity? = EntityUtils.getEntityByID(entity.id + offset)

    fun getArmorStandByRangeAll(entity: Entity, range: Double) =
        EntityUtils.getEntitiesNearby<ArmorStand>(entity.getLorenzVec(), range)

    fun getClosestArmorStand(entity: Entity, range: Double) =
        getArmorStandByRangeAll(entity, range).minByOrNull { it.distanceTo(entity) }

    fun getClosestArmorStandWithName(entity: Entity, range: Double, name: String) =
        getArmorStandByRangeAll(entity, range).filter { it.cleanName().startsWith(name) }.minByOrNull { it.distanceTo(entity) }

    fun ArmorStand.isDefaultValue() = this.name.formattedTextCompatLessResets() == defaultArmorStandName

    fun ArmorStand?.takeNonDefault() = this?.takeIf { !it.isDefaultValue() }

    fun ArmorStand.hasEmptyInventory() = getInventoryItems().none { it.isNotEmpty() }

    fun ArmorStand.isCompletelyDefault() = isDefaultValue() && hasEmptyInventory()

    class OwnerShip(val ownerName: String) {
        val ownerPlayer = MobData.players.firstOrNull { it.name == ownerName }
        override fun equals(other: Any?): Boolean {
            if (other is Player) return ownerPlayer == other || ownerName == other.name.formattedTextCompatLessResets()
            if (other is String) return ownerName == other
            return false
        }

        override fun hashCode(): Int {
            return ownerName.hashCode()
        }
    }

    fun rayTraceForMob(entity: Entity, distance: Double, partialTicks: Float, offset: LorenzVec = LorenzVec()) =
        rayTraceForMob(entity, partialTicks, offset)?.takeIf {
            it.baseEntity.distanceTo(entity.getLorenzVec()) <= distance
        }

    fun rayTraceForMobs(
        entity: Entity,
        distance: Double,
        partialTicks: Float,
        offset: LorenzVec = LorenzVec(),
    ) =
        rayTraceForMobs(entity, partialTicks, offset)?.filter {
            it.baseEntity.distanceTo(entity.getLorenzVec()) <= distance
        }.takeIf { it?.isNotEmpty() ?: false }

    fun rayTraceForMob(entity: Entity, partialTicks: Float, offset: LorenzVec = LorenzVec()) =
        rayTraceForMobs(entity, partialTicks, offset)?.firstOrNull()

    fun rayTraceForMobs(entity: Entity, partialTicks: Float, offset: LorenzVec = LorenzVec()): List<Mob>? {
        val look = entity.lookAngle.toLorenzVec().normalize()
        val pos = entity.eyePosition.toLorenzVec() + offset
        val possibleEntities = MobData.entityToMob.filterKeys {
            it !is ArmorStand &&
                it.boundingBox.rayIntersects(
                    pos, look,
                )
        }.values
        if (possibleEntities.isEmpty()) return null
        return possibleEntities.distinct().sortedBy { it.baseEntity.distanceTo(pos) }
    }

    val LivingEntity.mob: Mob? get() = MobData.entityToMob[this]

    val Entity.mob: Mob? get() = (this as? LivingEntity)?.mob

}
