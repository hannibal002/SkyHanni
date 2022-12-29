package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.EntityUtils.hasMaxHealth
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
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
    private val miniBosses = mutableListOf<EntityLivingBase>()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        if (tick++ % 20 == 0) {
            find()
        }
    }

    private fun find() {
        val entityList = Minecraft.getMinecraft().theWorld.loadedEntityList
        val list = mutableListOf<EntityLivingBase>()

        list.addAll(entityList.filterIsInstance<EntityZombie>().filter {
            it.hasMaxHealth(24_000) || it.hasMaxHealth(90_000) || it.hasMaxHealth(360_000) || it.hasMaxHealth(600_000) || it.hasMaxHealth(2_400_000)
        })

        list.addAll(entityList.filterIsInstance<EntitySpider>().filter {
            it.hasMaxHealth(54_000) || it.hasMaxHealth(144_000) || it.hasMaxHealth(576_000)
        })

        list.addAll(entityList.filterIsInstance<EntityWolf>().filter {
            it.hasMaxHealth(45_000) || it.hasMaxHealth(120_000) || it.hasMaxHealth(450_000)
        })

        list.addAll(entityList.filterIsInstance<EntityEnderman>().filter {
            it.hasMaxHealth(12_000_000) || it.hasMaxHealth(17_500_000) || it.hasMaxHealth(52_500_000)
        })

        list.addAll(entityList.filterIsInstance<EntityBlaze>().filter {
            it.hasMaxHealth(12_000_000) || it.hasMaxHealth(25_000_000)
        })

        list.filter { it !in miniBosses && !DamageIndicatorManager.isBoss(it) }.forEach(miniBosses::add)
        miniBosses.removeIf { DamageIndicatorManager.isBoss(it) }
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
    fun onResetEntityHurtTime(event: ResetEntityHurtEvent) {
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
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.slayer.slayerMinibossHighlight && !LorenzUtils.inDungeons && !LorenzUtils.inKuudraFight
    }
}
