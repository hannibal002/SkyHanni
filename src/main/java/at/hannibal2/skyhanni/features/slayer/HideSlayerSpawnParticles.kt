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
import net.minecraft.util.EnumParticleTypes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HideSlayerSpawnParticles {
    private val config get() = SlayerApi.config
    private var mobRecentDeaths = mutableListOf<Pair<LorenzVec, SimpleTimeMark>>()

    @HandleEvent(onlyOnSkyblock = true)
    @Suppress("MaxLineLength")
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!SlayerApi.hasActiveQuest() || !SlayerApi.isInCorrectArea) return
        val distance = event.location.distanceToNearestDeadMob() ?: return
        if (distance < 5) {
            ChatUtils.debug(config.spawnParticleHider.get().toString())
            if (config.spawnParticleHider.get().any { it.particle == event.type }) {
                event.cancel()
            }
        }

    }

    enum class SpawnParticles(private val displayName: String, val particle: EnumParticleTypes) {
        ENCHANT_TABLE("White", EnumParticleTypes.ENCHANTMENT_TABLE),
        SPELL_WITCH("Purple", EnumParticleTypes.SPELL_WITCH),
        SPELL_MOB("Slayer Specific", EnumParticleTypes.SPELL_MOB);

        override fun toString() = displayName
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

