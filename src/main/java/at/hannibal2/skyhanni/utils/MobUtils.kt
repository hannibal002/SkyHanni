package at.hannibal2.skyhanni.utils import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets

import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.rayIntersects
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.skyhanni.utils.compat.getInventoryItems
import net.minecraft.client.resource.language.I18n
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.player.PlayerEntity

@SkyHanniModule
object MobUtils {

    private val defaultArmorStandName get() =
        //#if MC < 1.21
        //$$ I18n.translate("entity.ArmorStand.name")
    //#else
    I18n.translate("entity.minecraft.armor_stand")
    //#endif

    // The corresponding ArmorStand for a mob has always the ID + 1 (with some exceptions)
    fun getArmorStand(entity: Entity, offset: Int = 1) = getNextEntity(entity, offset) as? ArmorStandEntity

    fun getNextEntity(entity: Entity, offset: Int): Entity? = EntityUtils.getEntityByID(entity.id + offset)

    fun getArmorStandByRangeAll(entity: Entity, range: Double) =
        EntityUtils.getEntitiesNearby<ArmorStandEntity>(entity.getLorenzVec(), range)

    fun getClosestArmorStand(entity: Entity, range: Double) =
        getArmorStandByRangeAll(entity, range).sortedBy { it.distanceTo(entity) }.firstOrNull()

    fun getClosestArmorStandWithName(entity: Entity, range: Double, name: String) =
        getArmorStandByRangeAll(entity, range).filter { it.cleanName().startsWith(name) }
            .sortedBy { it.distanceTo(entity) }.firstOrNull()

    fun ArmorStandEntity.isDefaultValue() = this.name.formattedTextCompatLessResets() == defaultArmorStandName

    fun ArmorStandEntity?.takeNonDefault() = this?.takeIf { !it.isDefaultValue() }

    fun ArmorStandEntity.hasEmptyInventory() = getInventoryItems().none { it.isNotEmpty() }

    fun ArmorStandEntity.isCompletelyDefault() = isDefaultValue() && hasEmptyInventory()

    class OwnerShip(val ownerName: String) {
        val ownerPlayer = MobData.players.firstOrNull { it.name == ownerName }
        override fun equals(other: Any?): Boolean {
            if (other is PlayerEntity) return ownerPlayer == other || ownerName == other.name.formattedTextCompatLessResets()
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
        //#if MC < 1.21
        //$$ val pos = entity.collidesWith(partialTicks).toLorenzVec() + offset
        //$$ val look = entity.getLook(partialTicks).toLorenzVec().normalize()
        //#else
        val look = entity.rotationVector.toLorenzVec().normalize()
        val pos = entity.eyePos.toLorenzVec() + offset
        //#endif
        val possibleEntities = MobData.entityToMob.filterKeys {
            it !is ArmorStandEntity &&
                it.boundingBox.rayIntersects(
                    pos, look
                )
        }.values
        if (possibleEntities.isEmpty()) return null
        return possibleEntities.distinct().sortedBy { it.baseEntity.distanceTo(pos) }
    }

    val LivingEntity.mob: Mob? get() = MobData.entityToMob[this]

    val Entity.mob: Mob? get() = (this as? LivingEntity)?.mob

}
