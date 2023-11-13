package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.data.EntityData
import at.hannibal2.skyhanni.utils.EntityUtils.isCorrupted
import at.hannibal2.skyhanni.utils.EntityUtils.isRunic
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzUtils.get
import at.hannibal2.skyhanni.utils.SkyblockMobUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand


abstract class SkyblockMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?, val extraEntities: List<EntityLivingBase>?) : SkyblockEntity(
    baseEntity, armorStand
) {
    val hologram1 by lazy { SkyblockMobUtils.getArmorStand(baseEntity, 2) }
    val hologram2 by lazy { SkyblockMobUtils.getArmorStand(baseEntity, 3) }

    init {
        LorenzDebug.log("SkyblockMob init block")
        LorenzDebug.log(extraEntities.toString())
        extraEntities?.count { EntityData.retries.contains(EntityData.RetryEntityInstancing(it, 0)) }?.also {
            EntityData.externRemoveOfRetryAmount += it
        }
    }
}

class SkyblockBasicMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?, extraEntities: List<EntityLivingBase>) : SkyblockMob(
    baseEntity, armorStand, extraEntities
) {
    private val regexResult = armorStand?.name?.removeColor()?.let { SkyblockMobUtils.mobNameFilter.find(it) }

    init {
        LorenzDebug.log("SkyblockBasicMob init block")
    }

    private fun removeCorruptedSuffix(string: String) =
        if (regexResult[3]?.isNotEmpty() == true) string.dropLast(1) else string

    override val name = regexResult[4]?.let { removeCorruptedSuffix(it) } ?: run {
        SkyblockMobUtils.errorNameFinding(baseEntity.name)
    }
    val level = regexResult[2]
    val isCorrupted get() = baseEntity.isCorrupted() // Can change
    val isRunic = baseEntity.isRunic() // Does not Change
}
