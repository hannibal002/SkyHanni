package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.minecraft.FovEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object SlayerFovChange {
    private val config get() = SlayerApi.config.fovChange

    @HandleEvent
    private fun onFov(event: FovEvent) {
        if (!isEnabled()) return
        event.setFov(config.targetFov)
    }

    fun isEnabled() = config.enabled && SlayerApi.isInBossFight()
}
