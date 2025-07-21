package at.hannibal2.skyhanni.utils.compat

import net.minecraft.util.DamageSource

object DamageSourceCompat {
    //#if MC < 1.21
    val cactus get(): DamageSource = DamageSource.cactus
    val drown get(): DamageSource = DamageSource.drown
    val fall get(): DamageSource = DamageSource.fall
    val generic get(): DamageSource = DamageSource.generic
    val inFire get(): DamageSource = DamageSource.inFire
    val inWall get(): DamageSource = DamageSource.inWall
    val lava get(): DamageSource = DamageSource.lava
    val lightningBolt get(): DamageSource = DamageSource.lightningBolt
    val magic get(): DamageSource = DamageSource.magic
    val onFire get(): DamageSource = DamageSource.onFire
    val outOfWorld get(): DamageSource = DamageSource.outOfWorld
    val starve get(): DamageSource = DamageSource.starve
    val wither get(): DamageSource = DamageSource.wither
    //#else
    //$$ private val damageSources = MinecraftCompat.localPlayer.world.damageSources
    //$$ val cactus get(): DamageSource = damageSources.cactus()
    //$$ val drown get(): DamageSource = damageSources.drown()
    //$$ val fall get(): DamageSource = damageSources.fall()
    //$$ val generic get(): DamageSource = damageSources.generic()
    //$$ val inFire get(): DamageSource = damageSources.inFire()
    //$$ val inWall get(): DamageSource = damageSources.inWall()
    //$$ val lava get(): DamageSource = damageSources.lava()
    //$$ val lightningBolt get(): DamageSource = damageSources.lightningBolt()
    //$$ val magic get(): DamageSource = damageSources.magic()
    //$$ val onFire get(): DamageSource = damageSources.onFire()
    //$$ val outOfWorld get(): DamageSource = damageSources.outOfWorld()
    //$$ val starve get(): DamageSource = damageSources.starve()
    //$$ val wither get(): DamageSource = damageSources.wither()
    //#endif
}
