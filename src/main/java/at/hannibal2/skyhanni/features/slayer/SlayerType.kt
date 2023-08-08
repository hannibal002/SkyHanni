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

    companion object {
        fun getByArea(skyBlockArea: String): SlayerType? {
            return when (skyBlockArea) {
                "Graveyard",
                "Coal Mine",
                -> REVENANT

                "Spider Mound",
                "Arachne's Burrow",
                "Arachne's Sanctuary",
                -> TARANTULA

                "Ruins",
                "Howling Cave",
                -> SVEN

                "The End",
                "Void Sepulture",
                "Zealot Bruiser Hideout",
                -> VOID

                "Stronghold",
                "The Wasteland",
                "Smoldering Tomb",
                -> INFERNO

                "Stillgore Château",
                "Oubliette",
                -> VAMPIRE

                else -> return null
            }
        }

        fun getByDisplayName(text: String): SlayerType? {
            return entries.firstOrNull { text.startsWith(it.displayName) }
        }
    }
}