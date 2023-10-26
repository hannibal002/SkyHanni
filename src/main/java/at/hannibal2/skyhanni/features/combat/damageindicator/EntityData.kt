package at.hannibal2.skyhanni.features.combat.damageindicator

import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.entity.EntityLivingBase

class EntityData(
    val entity: EntityLivingBase,
    var ignoreBlocks: Boolean,
    var delayedStart: Long,
    val finalDungeonBoss: Boolean,
    val bossType: BossType,
    val damageCounter: DamageCounter = DamageCounter(),
    val foundTime: Long,

    var lastHealth: Long = 0L,
    var healthText: String = "",
    var timeLastTick: Long = 0,
    var namePrefix: String = "",
    var nameSuffix: String = "",
    var nameAbove: String = "",
    var dead: Boolean = false,
    var deathLocation: LorenzVec? = null,
) {
    val timeToKill by lazy {
        val duration = System.currentTimeMillis() - foundTime
        "§e" + TimeUtils.formatDuration(duration, TimeUnit.SECOND, showMilliSeconds = true)
    }
}