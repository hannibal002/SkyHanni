package at.hannibal2.skyhanni.features.slayer

import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.monster.EntityBlaze
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntitySpider
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.passive.EntityWolf

enum class SlayerType(val displayName: String, val clazz: Class<*>) {
    REVENANT("Revenant Horror", EntityZombie::class.java),
    TARANTULA("Tarantula Broodfather", EntitySpider::class.java),
    SVEN("Sven Packmaster", EntityWolf::class.java),
    VOID("Voidgloom Seraph", EntityEnderman::class.java),
    INFERNO("Inferno Demonlord", EntityBlaze::class.java),
    VAMPIRE("Riftstalker Bloodfiend", EntityOtherPlayerMP::class.java)
    ;

    companion object
}
