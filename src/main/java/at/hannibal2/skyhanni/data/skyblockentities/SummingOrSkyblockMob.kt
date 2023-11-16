package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.data.EntityData
import at.hannibal2.skyhanni.utils.LocationUtils.union
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.AxisAlignedBB

abstract class SummingOrSkyblockMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?, private var extraEntitiesList: MutableList<EntityLivingBase>?) : SkyblockEntity(
    baseEntity, armorStand
) {
    private var relativeBoundingBox: AxisAlignedBB?
    val boundingBox: AxisAlignedBB
        get() = (relativeBoundingBox?.offset(baseEntity.posX, baseEntity.posY, baseEntity.posZ)
            ?: baseEntity.entityBoundingBox).expandBlock()

    val extraEntities: List<EntityLivingBase>? get() = extraEntitiesList

    init {
        removeExtraEntitiesFromChecking()
        relativeBoundingBox = makeRelativeBoundingBox()
    }

    private fun removeExtraEntitiesFromChecking() =
        extraEntities?.count { EntityData.retries.contains(EntityData.RetryEntityInstancing(it, 0)) }?.also {
            EntityData.externRemoveOfRetryAmount += it
        }

    private fun makeRelativeBoundingBox() =
        (baseEntity.entityBoundingBox.union(extraEntities?.filter { it !is EntityArmorStand }
            ?.mapNotNull { it.entityBoundingBox }))?.offset(-baseEntity.posX, -baseEntity.posY, -baseEntity.posZ)

    fun addEntityInFront(entity: EntityLivingBase) {
        extraEntitiesList?.add(0, entity) ?: run { extraEntitiesList = mutableListOf(entity) }
        relativeBoundingBox = makeRelativeBoundingBox()
        EntityData.putSummonOrSkyblockMob(entity, this)
    }

    fun addEntityInFront(entities: Collection<EntityLivingBase>) {
        extraEntitiesList?.addAll(0, entities) ?: run { extraEntitiesList = entities.toMutableList() }
        relativeBoundingBox = makeRelativeBoundingBox()
        removeExtraEntitiesFromChecking()
        EntityData.putAllSummonOrSkyblockMob(entities, this)
    }
}
