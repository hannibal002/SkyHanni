package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtTimeEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.monster.EntityBlaze
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntitySpider
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.passive.EntityWolf
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class HighlightSlayerMiniboss {

    private var tick = 0
    private val miniBosses = mutableListOf<Entity>()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        if (tick++ % 20 == 0) {
            find()
        }
    }

    private fun find() {
        val entityList = Minecraft.getMinecraft().theWorld.loadedEntityList
        val list = mutableListOf<Entity>()

        list.addAll(entityList.filterIsInstance<EntityZombie>().filter {
            it.baseMaxHealth % 24_000 == 0.0 || it.baseMaxHealth % 90_000 == 0.0 || it.baseMaxHealth % 360_000 == 0.0 || it.baseMaxHealth % 600_000 == 0.0 || it.baseMaxHealth % 2_400_000 == 0.0
        })

        list.addAll(entityList.filterIsInstance<EntitySpider>().filter {
            it.baseMaxHealth % 54_000 == 0.0 || it.baseMaxHealth % 144_000 == 0.0 || it.baseMaxHealth % 576_000 == 0.0
        })

        list.addAll(entityList.filterIsInstance<EntityWolf>().filter {
            it.baseMaxHealth % 45_000 == 0.0 || it.baseMaxHealth % 120_000 == 0.0 || it.baseMaxHealth % 450_000 == 0.0
        })

        list.addAll(entityList.filterIsInstance<EntityEnderman>().filter {
            it.baseMaxHealth % 12_000_000 == 0.0 || it.baseMaxHealth % 25_000_000 == 0.0
        })

        list.addAll(entityList.filterIsInstance<EntityBlaze>().filter {
            it.baseMaxHealth % 12_000_000 == 0.0 || it.baseMaxHealth % 25_000_000 == 0.0
        })

        list.filter { it !in miniBosses }.forEach(miniBosses::add)
    }

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!isEnabled()) return
        val entity = event.entity

        if (entity in miniBosses) {
            event.color = LorenzColor.AQUA.toColor().withAlpha(127)
        }
    }

    @SubscribeEvent
    fun onResetEntityHurtTime(event: ResetEntityHurtTimeEvent) {
        if (!isEnabled()) return
        val entity = event.entity

        if (entity in miniBosses) {
            event.shouldReset = true
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        miniBosses.clear()
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.misc.slayerMinibossHighlight && !LorenzUtils.inDungeons && !LorenzUtils.inKuudraFight
    }
}