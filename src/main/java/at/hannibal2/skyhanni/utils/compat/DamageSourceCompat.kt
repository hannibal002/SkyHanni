package at.hannibal2.skyhanni.utils.compat

//#if MC < 1.21
import net.minecraft.util.DamageSource
//#else
//$$ import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
//#endif

object DamageSourceCompat {
    //#if MC < 1.21
    val cactus get() = DamageSource.cactus
    val drown get() = DamageSource.drown
    val fall get() = DamageSource.fall
    val generic get() = DamageSource.generic
    val inFire get() = DamageSource.inFire
    val inWall get() = DamageSource.inWall
    val lava get() = DamageSource.lava
    val lightningBolt get() = DamageSource.lightningBolt
    val magic get() = DamageSource.magic
    val onFire get() = DamageSource.onFire
    val outOfWorld get() = DamageSource.outOfWorld
    val starve get() = DamageSource.starve
    val wither get() = DamageSource.wither
    //#else
    //$$ private val damageSources = MinecraftCompat.localPlayer.world.getDamageSources()
    //$$ val cactus get() = damageSources.cactus()
    //$$ val drown get() = damageSources.drown()
    //$$ val fall get() = damageSources.fall()
    //$$ val generic get() = damageSources.generic()
    //$$ val inFire get() = damageSources.inFire()
    //$$ val inWall get() = damageSources.inWall()
    //$$ val lava get() = damageSources.lava()
    //$$ val lightningBolt get() = damageSources.lightningBolt()
    //$$ val magic get() = damageSources.magic()
    //$$ val onFire get() = damageSources.onFire()
    //$$ val outOfWorld get() = damageSources.outOfWorld()
    //$$ val starve get() = damageSources.starve()
    //$$ val wither get() = damageSources.wither()
    //#endif

}
