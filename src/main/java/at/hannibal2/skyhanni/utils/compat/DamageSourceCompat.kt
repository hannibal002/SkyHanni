package at.hannibal2.skyhanni.utils.compat

import net.minecraft.world.damagesource.DamageSource

object DamageSourceCompat {
    private val damageSources = MinecraftCompat.localWorld.damageSources()
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
    val outOfWorld get(): DamageSource = damageSources.fellOutOfWorld()
    val starve get(): DamageSource = damageSources.starve()
    val wither get(): DamageSource = damageSources.wither()
}
