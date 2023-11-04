package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.data.EntityData
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.SkyblockMobUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand

abstract class SkyblockEntity(val baseEntity: Entity, val armorStand: EntityArmorStand? = SkyblockMobUtils.getArmorStand(baseEntity)) {

    // If an entity has a hologram (second ArmorStand) it has the ID + 2 if not there will be another mob
    val hologram by lazy {
        EntityUtils.getEntityByID(baseEntity.entityId + 2)?.takeIf { it is EntityArmorStand } as? EntityArmorStand
    }

    abstract val name: String

    override fun toString() = name

    override fun hashCode(): Int {
        return baseEntity.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is SkyblockEntity) {
            return baseEntity == other.baseEntity
        }

        if (other is Entity) {
            return baseEntity == other
        }

        return false
    }

    fun isInRender() = baseEntity.distanceToPlayer() < EntityData.ENTITY_RENDER_RANGE_IN_BLOCKS
}
