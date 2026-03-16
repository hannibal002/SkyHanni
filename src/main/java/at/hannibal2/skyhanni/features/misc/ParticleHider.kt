package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball

@SkyHanniModule
object ParticleHider {

    private val config get() = SkyHanniMod.feature.misc.particleHiders

    private val smokeTypes = setOf(
        ParticleTypes.SMOKE,
        ParticleTypes.LARGE_SMOKE,
    )
    private fun inM7Boss() = DungeonApi.inDungeon() && DungeonApi.dungeonFloor == "M7" && DungeonApi.inBossRoom

    @HandleEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!MinecraftCompat.localPlayerExists) return
        with(event) {
            val hideFarCancel = (config.hideFarParticles && distanceToPlayer > 40 && !inM7Boss())
            val hideCloseRedstoneCancel = (config.hideCloseRedstoneParticles && type == ParticleTypes.DUST && distanceToPlayer < 2)
            val hideFireballCancel = config.hideFireballParticles && type in smokeTypes &&
                event.location.getEntitiesNearby<SmallFireball>(5.0).isNotEmpty()

            if (hideFarCancel || hideCloseRedstoneCancel || hideFireballCancel) event.cancel()
        }
    }

    @JvmStatic
    fun shouldHideBlockParticles(): Boolean {
        val config = config.blockBreakParticle
        return when {
            !config.hide -> false
            config.onlyInGarden -> IslandType.GARDEN.isInIsland()
            else -> true
        }
    }

    @JvmStatic
    fun shouldHideFireParticles() = MinecraftCompat.localWorldExists && config.hideFireBlockParticles

    @JvmStatic
    fun shouldHideBlazeParticles() = MinecraftCompat.localWorldExists && config.hideBlazeParticles

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.hideBlazeParticles", "misc.particleHiders.hideBlazeParticles")
        event.move(3, "misc.hideEndermanParticles", "misc.particleHiders.hideEndermanParticles")
        event.move(3, "misc.hideFarParticles", "misc.particleHiders.hideFarParticles")
        event.move(3, "misc.hideFireballParticles", "misc.particleHiders.hideFireballParticles")
        event.move(3, "misc.hideCloseRedstoneparticles", "misc.particleHiders.hideCloseRedstoneParticles")
        event.move(3, "misc.hideFireBlockParticles", "misc.particleHiders.hideFireBlockParticles")
        event.move(3, "misc.hideSmokeParticles", "misc.particleHiders.hideSmokeParticles")
    }
}
