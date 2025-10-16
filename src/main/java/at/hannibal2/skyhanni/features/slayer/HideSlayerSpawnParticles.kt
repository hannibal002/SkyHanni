package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.getLorenzVec
import kotlin.time.Duration.Companion.seconds
import net.minecraft.util.EnumParticleTypes as ParticleType

@SkyHanniModule
object HideSlayerSpawnParticles {
    private val config get() = SlayerApi.config

    @Suppress("VarCouldBeVal")
    private var mobRecentDeaths = mutableListOf<Pair<LorenzVec, SimpleTimeMark>>()

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!SlayerApi.hasActiveQuest() || !SlayerApi.isInCorrectArea) return
        val distance = event.location.distanceToNearestDeadMob() ?: return
        if (distance < 5) {

            ChatUtils.debug(config.spawnParticleHider.get().toString())
            if (shouldHide(event.type)) {
                event.cancel()
            }
        }
    }

    enum class SpawnParticles(private val displayName: String) {
        ENCHANT_TABLE("White"),
        SPELL_WITCH("Purple"),
        SPELL_MOB("Slayer Specific");

        override fun toString() = displayName
    }

    @Suppress("MaxLineLength")
    private fun shouldHide(particle: ParticleType): Boolean {
        if (config.spawnParticleHider.get().contains(SpawnParticles.ENCHANT_TABLE) && particle.particleID == ParticleType.ENCHANTMENT_TABLE.particleID
        ) {
            return true
        }
        if (config.spawnParticleHider.get().contains(SpawnParticles.SPELL_WITCH) && particle.particleID == ParticleType.SPELL_WITCH.particleID
        ) {
            return true
        }
        return config.spawnParticleHider.get().contains(SpawnParticles.SPELL_MOB) && particle.particleID == ParticleType.SPELL_MOB.particleID
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (event.health.toDouble() != 0.0) return
        val entity = event.entity
        mobRecentDeaths.add(Pair(entity.getLorenzVec(), SimpleTimeMark.now()))
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        val iterator = mobRecentDeaths.iterator()
        while (iterator.hasNext()) {
            val element = iterator.next()
            if (element.second.passedSince() > 3.seconds) {
                iterator.remove()
            }
        }

    }

    private fun LorenzVec.distanceToNearestDeadMob() = mobRecentDeaths.minOfOrNull { it.first.distanceSq(this) }
}

