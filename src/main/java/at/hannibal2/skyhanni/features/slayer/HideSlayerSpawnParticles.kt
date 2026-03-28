package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.phys.Vec3
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HideSlayerSpawnParticles {
    private val config get() = SlayerApi.config

    private val mobRecentDeaths = mutableMapOf<Vec3, SimpleTimeMark>()

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!SlayerApi.hasActiveQuest() || !SlayerApi.isInCorrectArea) return
        val distance = event.location.distanceToNearestDeadMob() ?: return
        if (distance >= 5) return
        if (config.spawnParticleHider.get().any { it.particle == event.type }) {
            event.cancel()
        }
    }

    enum class SpawnParticles(private val displayName: String, val particle: ParticleType<*>) {
        ENCHANT_TABLE("White", ParticleTypes.ENCHANT),
        SPELL_WITCH("Purple", ParticleTypes.WITCH),
        SPELL_MOB("Slayer Specific", ParticleTypes.ENTITY_EFFECT);

        override fun toString() = displayName
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (event.health.toDouble() != 0.0) return
        mobRecentDeaths[event.entity.position()] = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        mobRecentDeaths.removeIf { it.value.passedSince() > 3.seconds }
    }

    private fun Vec3.distanceToNearestDeadMob() =
        mobRecentDeaths.minOfOrNull { distanceToSqr(it.key) }
}

