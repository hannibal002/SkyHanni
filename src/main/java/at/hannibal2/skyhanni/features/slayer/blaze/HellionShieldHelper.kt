package at.hannibal2.hanni.features.slayer.blaze

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.SlayerApi
import at.hannibal2.hanni.events.DebugDataCollectEvent
import at.hannibal2.hanni.events.entity.EntityDeathEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.addAlpha
import at.hannibal2.hanni.utils.PlayerUtils
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.getLorenzVec
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawDynamicText
import net.minecraft.entity.EntityLiving

@HanniModule
object HellionShieldHelper {

    val hellionShieldMobs = mutableMapOf<EntityLiving, HellionShield>()

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "slayer.blazeColoredMobs", "slayer.blazes.hellion.coloredMobs")
    }

    @HandleEvent
    fun onEntityDeath(event: EntityDeathEvent<*>) {
        hellionShieldMobs.remove(event.entity)
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Hellion Shield")
        event.addIrrelevant {
            add("hellionShieldMobs: ${hellionShieldMobs.size}")
            for ((entity, type) in hellionShieldMobs) {
                add("${entity.getLorenzVec()} - $type")
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!SkyBlockUtils.debug || !PlayerUtils.isSneaking()) return

        for ((entity, type) in hellionShieldMobs) {
            event.drawDynamicText(entity.getLorenzVec().add(y = 2), type.cleanName, 1.5, seeThroughBlocks = false)
        }
    }

    @HandleEvent
    fun onWorldChange() {
        hellionShieldMobs.clear()
    }

    fun EntityLiving.setHellionShield(shield: HellionShield?) {
        shield?.let {
            hellionShieldMobs[this] = it
            RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
                this,
                it.color.toColor().addAlpha(80),
            ) { SkyBlockUtils.inSkyBlock && SlayerApi.config.blazes.hellion.coloredMobs }
        } ?: run {
            hellionShieldMobs.remove(this)
            RenderLivingEntityHelper.removeCustomRender(this)
        }
    }
}
