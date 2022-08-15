package at.hannibal2.skyhanni.damageindicator

import net.minecraft.entity.EntityLivingBase

class EntityData(
    val entity: EntityLivingBase,
    var ignoreBlocks: Boolean,
    var delayedStart: Long,
    val finalDungeonBoss: Boolean,
    val bossType: BossType,

    var lastHealth: Int = 0,
    var healthText: String = "",
    var timeLastTick: Long = 0,
    var healthLineHidden: Boolean = false,
    var namePrefix: String = "",
    var nameSuffix: String = "",
    var dead: Boolean = false
)