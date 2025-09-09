package at.hannibal2.skyhanni.utils.compat

import net.minecraft.entity.damage.DamageSource

object DamageSourceCompat {
    //#if MC < 1.21
    //$$ val cactus get(): DamageSource = DamageSource.CACTUS
    //$$ val drown get(): DamageSource = DamageSource.DROWN
    //$$ val fall get(): DamageSource = DamageSource.FALL
    //$$ val generic get(): DamageSource = DamageSource.GENERIC
    //$$ val inFire get(): DamageSource = DamageSource.IN_FIRE
    //$$ val inWall get(): DamageSource = DamageSource.IN_WALL
    //$$ val lava get(): DamageSource = DamageSource.LAVA
    //$$ val lightningBolt get(): DamageSource = DamageSource.LIGHTNING_BOLT
    //$$ val magic get(): DamageSource = DamageSource.MAGIC
    //$$ val onFire get(): DamageSource = DamageSource.ON_FIRE
    //$$ val outOfWorld get(): DamageSource = DamageSource.OUT_OF_WORLD
    //$$ val starve get(): DamageSource = DamageSource.STARVE
    //$$ val wither get(): DamageSource = DamageSource.WITHER
    //#else
    private val damageSources = MinecraftCompat.localPlayer.world.damageSources
    val cactus get(): DamageSource = damageSources.cactus()
    val drown get(): DamageSource = damageSources.drown()
    val fall get(): DamageSource = damageSources.fall()
    val generic get(): DamageSource = damageSources.generic()
    val inFire get(): DamageSource = damageSources.inFire()
    val inWall get(): DamageSource = damageSources.inWall()
    val lava get(): DamageSource = damageSources.lava()
    val lightningBolt get(): DamageSource = damageSources.lightningBolt()
    val magic get(): DamageSource = damageSources.magic()
    val onFire get(): DamageSource = damageSources.onFire()
    val outOfWorld get(): DamageSource = damageSources.outOfWorld()
    val starve get(): DamageSource = damageSources.starve()
    val wither get(): DamageSource = damageSources.wither()
    //#endif
}
