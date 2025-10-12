package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.slayer.SlayerConfig.SlayerSpawnParticlesToHide as ToHide
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.slayer.SlayerStateChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.util.EnumParticleTypes as ParticleType

@SkyHanniModule
object HideSlayerSpawnParticles {
    private val config get() = SlayerApi.config
    private var isGrinding = false

    @HandleEvent(onlyOnSkyblock = true)
    @Suppress("MaxLineLength")
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!SlayerApi.hasActiveQuest() && !isGrinding) return

        val type = event.type
        val slayerParticle = config.slayerParticle
        if (type == ParticleType.ENCHANTMENT_TABLE && slayerParticle == ToHide.BOTH) {
            event.cancel()
        }
        if ((type == ParticleType.SPELL_WITCH || type == ParticleType.SPELL_MOB) && (slayerParticle == ToHide.BOTH || slayerParticle == ToHide.PURPLE)) {
            event.cancel()
        }
    }
    @HandleEvent(onlyOnSkyblock = true)
    fun onSlayerStateChange(event: SlayerStateChangeEvent) {
        isGrinding = event.state == SlayerApi.ActiveQuestState.GRINDING
    }

}
