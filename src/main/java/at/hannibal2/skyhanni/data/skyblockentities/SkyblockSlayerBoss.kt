package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyblockMobUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val regex = "Spawned by: (.*)".toRegex()

open class SkyblockSlayerBoss(baseEntity: Entity, armorStand: EntityArmorStand?, nameS: String, tierS: String) : SkyblockBossMob(baseEntity, armorStand, nameS) {
    val tier = tierS.romanToDecimal()

    private val timeArmorStand = SkyblockMobUtils.getArmorStand(baseEntity, 2)
    private val ownerArmorStand = SkyblockMobUtils.getArmorStand(baseEntity, 3)

    val owner = SkyblockMobUtils.ownerShip(
        regex.find(ownerArmorStand?.name?.removeColor() ?: "")?.groupValues?.get(1) ?: ""
    )

    val timeUntilDeSpawn: SimpleTimeMark? = run {
        if (timeArmorStand == null) return@run null
        val timeString = timeArmorStand.name.removeColor().split(":").map { it.toInt() }
        val duration =
            timeString[0].toDuration(DurationUnit.MINUTES).plus(timeString[1].toDuration(DurationUnit.SECONDS))
        return@run SimpleTimeMark.now().plus(duration)
    }
}
