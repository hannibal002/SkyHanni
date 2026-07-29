package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.ParticleEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.particles.ParticleTypes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HideSlayerSpawnParticles {

    private val config get() = SlayerApi.config

    private val mobRecentDeaths = TimeLimitedCache<LorenzVec, SimpleTimeMark>(3.seconds)

    @HandleEvent(onlyOnSkyblock = true)
    fun onParticle(event: ParticleEvent) {
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
        mobRecentDeaths[event.entity.getLorenzVec()] = SimpleTimeMark.now()
    }

    private fun LorenzVec.distanceToNearestDeadMob() = mobRecentDeaths.minOfOrNull { it.key.distanceSq(this) }
}
