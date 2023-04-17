package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class HideMobNames {

    private val lastMobName = mutableMapOf<EntityArmorStand, String>()
    private val mobNamesHidden = mutableListOf<EntityArmorStand>()
    private val patterns = mutableListOf<Pattern>()

    init {
        addMobToHide("Zombie")
        addMobToHide("Zombie")
        addMobToHide("Zombie Villager")
        addMobToHide("Crypt Ghoul")

        addMobToHide("Dasher Spider")
        addMobToHide("Weaver Spider")
        addMobToHide("Splitter Spider")
        addMobToHide("Voracious Spider")
        addMobToHide("Silverfish")

        addMobToHide("Wolf")
        addMobToHide("§bHowling Spirit")
        addMobToHide("§bPack Spirit")

        addMobToHide("Enderman")
        addMobToHide("Voidling Fanatic")

        addMobToHide("Blaze") // 1.2m
        addMobToHide("Mutated Blaze") // 1.5m
        addMobToHide("Bezal") // 2m
        addMobToHide("Smoldering Blaze") // 5.5m
    }

    private fun addMobToHide(bossName: String) {
        patterns.add("§8\\[§7Lv(\\d+)§8] §c$bossName§r §[ae](.+)§f/§a(.+)§c❤".toPattern())
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: RenderLivingEvent.Specials.Pre<EntityLivingBase>) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.slayer.hideMobNames) return

        val entity = event.entity
        if (entity !is EntityArmorStand) return
        if (!entity.hasCustomName()) return

        val name = entity.name
        if (lastMobName.getOrDefault(entity, "abc") == name) {
            if (entity in mobNamesHidden) {
                event.isCanceled = true
            }
            return
        }

        lastMobName[entity] = name
        mobNamesHidden.remove(entity)

        if (shouldNameBeHidden(name)) {
            event.isCanceled = true
            mobNamesHidden.add(entity)
        }
    }

    private fun shouldNameBeHidden(name: String): Boolean {
        for (pattern in patterns) {
            val matcher = pattern.matcher(name)
            if (matcher.matches()) {
                val min = matcher.group(2)
                val max = matcher.group(3)
                if (min == max || min == "0") {
                    return true
                }
            }
        }

        return false
    }
}