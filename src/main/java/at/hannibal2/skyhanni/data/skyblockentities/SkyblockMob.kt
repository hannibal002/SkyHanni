package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.data.EntityData
import at.hannibal2.skyhanni.data.MobFilter
import at.hannibal2.skyhanni.utils.EntityUtils.isCorrupted
import at.hannibal2.skyhanni.utils.EntityUtils.isRunic
import at.hannibal2.skyhanni.utils.LocationUtils.union
import at.hannibal2.skyhanni.utils.LorenzUtils.get
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.AxisAlignedBB


abstract class SkyblockMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?, extraEntitiesList: MutableList<EntityLivingBase>?) : SummingOrSkyblockMob(
    baseEntity, armorStand, extraEntitiesList
) {

    val hologram1 by lazy { MobUtils.getArmorStand(baseEntity, 2) }
    val hologram2 by lazy { MobUtils.getArmorStand(baseEntity, 3) }

}

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

class SkyblockBasicMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?, extraEntities: List<EntityLivingBase>) : SkyblockMob(
    baseEntity, armorStand, extraEntities.toMutableList()
) {
    private val regexResult = armorStand?.name?.removeColor()?.let { MobFilter.mobNameFilter.find(it) }

    private fun removeCorruptedSuffix(string: String) =
        if (regexResult[3]?.isNotEmpty() == true) string.dropLast(1) else string

    override val name = regexResult[4]?.let { removeCorruptedSuffix(it) } ?: run {
        MobFilter.errorNameFinding(baseEntity.name)
    }
    val level = regexResult[2]
    val isCorrupted get() = baseEntity.isCorrupted() // Can change
    val isRunic = baseEntity.isRunic() // Does not Change
}
