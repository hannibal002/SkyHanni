package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.utils.LorenzUtils.get
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val regex = "Spawned by: (.*)".toRegex()

open class SkyblockSlayerBoss(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?, nameS: String, tierS: String, extraEntityList: List<EntityLivingBase>) : SkyblockBossMob(baseEntity, armorStand, nameS, extraEntityList) {
    val tier = tierS.romanToDecimal()

    private val timeArmorStand = hologram1
    private val ownerArmorStand = hologram2

    val owner = MobUtils.OwnerShip(
        regex.find(ownerArmorStand?.name?.removeColor() ?: "")[1] ?: ""
    )

    val timeUntilDeSpawn: SimpleTimeMark? = run {
        if (timeArmorStand == null) return@run null
        val timeString = timeArmorStand.name.removeColor().split(":").map { it.toInt() }
        val duration =
            timeString[0].toDuration(DurationUnit.MINUTES).plus(timeString[1].toDuration(DurationUnit.SECONDS))
        return@run SimpleTimeMark.now().plus(duration)
    }
}
