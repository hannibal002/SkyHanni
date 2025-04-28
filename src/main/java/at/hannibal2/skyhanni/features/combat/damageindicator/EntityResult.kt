package at.hannibal2.skyhanni.features.combat.damageindicator

import at.hannibal2.skyhanni.utils.SimpleTimeMark

class EntityResult(
    val delayedStart: SimpleTimeMark? = null,
    val ignoreBlocks: Boolean = false,
    val finalDungeonBoss: Boolean = false,
    val bossType: BossType,
)
