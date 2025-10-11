package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.slayer.SlayerConfig.SlayerSpawnParticlestoHide
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.slayer.SlayerStateChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.util.EnumParticleTypes

@SkyHanniModule
object HideSlayerSpawnParticles {
    private val config get() = SlayerApi.config
    private var isGrinding = false

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!SlayerApi.hasActiveQuest() && !isGrinding) return

        if (event.type == EnumParticleTypes.ENCHANTMENT_TABLE && config.slayerParticle == SlayerSpawnParticlestoHide.BOTH) {
            event.cancel()
        }
        if ((event.type == EnumParticleTypes.SPELL_WITCH || event.type == EnumParticleTypes.SPELL_MOB) && (config.slayerParticle == SlayerSpawnParticlestoHide.BOTH || config.slayerParticle == SlayerSpawnParticlestoHide.PURPLE)) {
            event.cancel()
        }
    }
    @HandleEvent(onlyOnSkyblock = true)
    fun onSlayerStateChange(event: SlayerStateChangeEvent) {
        isGrinding = event.state == SlayerApi.ActiveQuestState.GRINDING
    }

}
