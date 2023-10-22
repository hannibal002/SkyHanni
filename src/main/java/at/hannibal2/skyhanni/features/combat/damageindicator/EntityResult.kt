package at.hannibal2.skyhanni.features.combat.damageindicator

class EntityResult(
    val delayedStart: Long = -1L,
    val ignoreBlocks: Boolean = false,
    val finalDungeonBoss: Boolean = false,
    val bossType: BossType = BossType.GENERIC_DUNGEON_BOSS,
)