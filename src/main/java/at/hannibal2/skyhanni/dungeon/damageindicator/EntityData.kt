package at.hannibal2.skyhanni.dungeon.damageindicator

import at.hannibal2.skyhanni.utils.LorenzColor
import net.minecraft.entity.EntityLivingBase

class EntityData(
    val entity: EntityLivingBase,
    var ignoreBlocks: Boolean,
    val delayedStart: Long,
    val finalDungeonBoss: Boolean,
    val bossType: BossType = BossType.DUNGEON,

    var lastHealth: Int = 0,
    var text: String = "",
    var color: LorenzColor = LorenzColor.DARK_GREEN,
    var timeLastTick: Long = 0,
)