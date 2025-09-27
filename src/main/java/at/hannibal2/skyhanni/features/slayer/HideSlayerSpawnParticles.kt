package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.util.EnumParticleTypes

@SkyHanniModule
object HideSlayerSpawnParticles {
    private val config get() = SlayerApi.config

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!SlayerApi.hasActiveSlayerQuest()) return
        if (event.type == EnumParticleTypes.ENCHANTMENT_TABLE && config.whiteParticleHider) {
            event.cancel()
        }
        if ((event.type == EnumParticleTypes.SPELL_WITCH || event.type == EnumParticleTypes.SPELL_MOB ) && config.purpleParticleHider) {
            event.cancel()
        }
    }
}
