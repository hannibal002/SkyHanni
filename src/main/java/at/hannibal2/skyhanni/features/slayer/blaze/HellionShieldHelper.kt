package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.entity.EntityDeathEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import net.minecraft.world.entity.Mob

@SkyHanniModule
object HellionShieldHelper {

    val hellionShieldMobs = mutableMapOf<Mob, HellionShield>()

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
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!SkyBlockUtils.debug || !PlayerUtils.isSneaking()) return

        for ((entity, type) in hellionShieldMobs) {
            event.drawDynamicText(entity.getLorenzVec().add(y = 2), type.cleanName, 1.5, seeThroughBlocks = false)
        }
    }

    @HandleEvent
    fun onWorldChange() {
        hellionShieldMobs.clear()
    }

    fun Mob.setHellionShield(shield: HellionShield?) {
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
