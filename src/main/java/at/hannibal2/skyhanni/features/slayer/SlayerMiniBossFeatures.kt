package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.combat.CocoonSpawnEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.compat.EntityCompat.deceased
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToCrosshair
import net.minecraft.world.entity.Entity

@SkyHanniModule
object SlayerMiniBossFeatures {

    private val config get() = SlayerApi.config.miniboss
    private var miniBosses = mutableSetOf<Mob>()
    private var cocoons = mutableSetOf<Entity>()

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        if (!SlayerMiniBossType.isMiniboss(mob.name)) return
        miniBosses += mob
        if (config.slayerMinibossHighlight) mob.highlight(config.minibossLine.color)
    }

    @HandleEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        miniBosses -= event.mob
    }

    @HandleEvent
    fun onCocoonSpawn(event: CocoonSpawnEvent) {
        val cocoon = event.cocoonMob
        if (!SlayerMiniBossType.isMiniboss(cocoon.mob.name)) return
        cocoons += cocoon.cocoonEntity
        RenderLivingEntityHelper.setEntityColor(cocoon.cocoonEntity, config.cocoonLine.color.toColor()) {
            config.cocoonHighlight
        }
    }

    @HandleEvent
    fun onSecondPassed() {
        cocoons.removeIf { it.deceased }
        miniBosses.removeIf { it.baseEntity.deceased }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!SlayerApi.isInAnyArea) return
        if (config.minibossLine.showLine && !(SlayerApi.isInBossFight() && config.shouldBossInterruptLine)) {
            for (mob in miniBosses) {
                if (!mob.baseEntity.canBeSeen(10)) continue
                event.drawLineToCrosshair(
                    mob.baseEntity.getLorenzVec().up(),
                    config.minibossLine.color,
                    config.minibossLine.lineWidth,
                    true,
                )
            }
        }
        if (config.cocoonLine.showLine && !(SlayerApi.isInBossFight() && config.shouldBossInterruptCocoonLine)) {
            for (mob in cocoons) {
                if (!mob.canBeSeen(10)) continue
                event.drawLineToCrosshair(
                    mob.getLorenzVec().up(),
                    config.cocoonLine.color,
                    config.cocoonLine.lineWidth,
                    true,
                )
            }
        }
    }
}
