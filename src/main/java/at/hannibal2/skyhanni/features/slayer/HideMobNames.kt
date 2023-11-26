package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
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
        // TODO USE SH-REPO
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
        patterns.add("§8\\[§7Lv(?:\\d+)§8] §c$bossName§r §[ae](?<min>.+)§f/§a(?<max>.+)§c❤".toPattern())
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
            pattern.matchMatcher(name) {
                val min = group("min")
                val max = group("max")
                if (min == max || min == "0") {
                    return true
                }
            }
        }

        return false
    }
}
