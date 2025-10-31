package at.hannibal2.hanni.features.slayer

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.SlayerApi
import at.hannibal2.hanni.events.ReceiveParticleEvent
import at.hannibal2.hanni.events.entity.EntityHealthUpdateEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.hanni.utils.getLorenzVec
import net.minecraft.util.EnumParticleTypes
import kotlin.time.Duration.Companion.seconds

@HanniModule
object HideSlayerSpawnParticles {
    private val config get() = SlayerApi.config

    @Suppress("VarCouldBeVal")
    private var mobRecentDeaths = mutableMapOf<LorenzVec, SimpleTimeMark>()

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!SlayerApi.hasActiveQuest() || !SlayerApi.isInCorrectArea) return
        val distance = event.location.distanceToNearestDeadMob() ?: return
        if (distance >= 5) return

        if (config.spawnParticleHider.get().any { it.particle.check(event) }) {
            event.cancel()
        }
    }

    enum class SpawnParticles(private val displayName: String, val particle: FakeParticleType) {
        ENCHANT_TABLE("White", FakeParticleType.ENCHANT),
        SPELL_WITCH("Purple", FakeParticleType.WITCH),
        SPELL_MOB("Slayer Specific", FakeParticleType.SPECIFIC);

        override fun toString() = displayName
    }

    // TODO This is literally just copied from GriffinBurrowParticleFinder, should be ParticleUtils in the future
    enum class FakeParticleType(val check: ReceiveParticleEvent.() -> Boolean) {
        ENCHANT({ type == EnumParticleTypes.ENCHANTMENT_TABLE }),
        WITCH({ type == EnumParticleTypes.SPELL_WITCH }),
        SPECIFIC({ type == EnumParticleTypes.SPELL_MOB }),
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (event.health.toDouble() != 0.0) return
        mobRecentDeaths[event.entity.getLorenzVec()] = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        mobRecentDeaths.removeIf { it.value.passedSince() > 3.seconds }
    }

    private fun LorenzVec.distanceToNearestDeadMob() = mobRecentDeaths.minOfOrNull { it.key.distanceSq(this) }
}

