package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.data.EntityData
import at.hannibal2.skyhanni.utils.EntityUtils.isCorrupted
import at.hannibal2.skyhanni.utils.EntityUtils.isRunic
import at.hannibal2.skyhanni.utils.LocationUtils.union
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzUtils.get
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.SkyblockMobUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.AxisAlignedBB


abstract class SkyblockMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?, val extraEntities: List<EntityLivingBase>?) : SkyblockEntity(
    baseEntity, armorStand
) {
    val hologram1 by lazy { SkyblockMobUtils.getArmorStand(baseEntity, 2) }
    val hologram2 by lazy { SkyblockMobUtils.getArmorStand(baseEntity, 3) }

    private val relativeBoundingBox: AxisAlignedBB
    val boundingBox: AxisAlignedBB
        get() = relativeBoundingBox.offset(baseEntity.posX, baseEntity.posY, baseEntity.posZ).expandBlock()

    init {
        extraEntities?.count { EntityData.retries.contains(EntityData.RetryEntityInstancing(it, 0)) }?.also {
            EntityData.externRemoveOfRetryAmount += it
        }
        relativeBoundingBox = (baseEntity.entityBoundingBox.union(extraEntities?.mapNotNull { it.entityBoundingBox })
            ?: baseEntity.entityBoundingBox).offset(-baseEntity.posX, -baseEntity.posY, -baseEntity.posZ)
        LorenzDebug.log(relativeBoundingBox.toString())
    }
}

class SkyblockBasicMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?, extraEntities: List<EntityLivingBase>) : SkyblockMob(
    baseEntity, armorStand, extraEntities
) {
    private val regexResult = armorStand?.name?.removeColor()?.let { SkyblockMobUtils.mobNameFilter.find(it) }

    private fun removeCorruptedSuffix(string: String) =
        if (regexResult[3]?.isNotEmpty() == true) string.dropLast(1) else string

    override val name = regexResult[4]?.let { removeCorruptedSuffix(it) } ?: run {
        SkyblockMobUtils.errorNameFinding(baseEntity.name)
    }
    val level = regexResult[2]
    val isCorrupted get() = baseEntity.isCorrupted() // Can change
    val isRunic = baseEntity.isRunic() // Does not Change
}
