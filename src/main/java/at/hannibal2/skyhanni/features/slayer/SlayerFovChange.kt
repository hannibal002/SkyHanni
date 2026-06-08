package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.minecraft.BaseFovEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object SlayerFovChange {
    private val config get() = SlayerApi.config.fovChange

    @HandleEvent
    fun onFov(event: BaseFovEvent) {
        if (!isEnabled()) return
        event.setFov(config.targetFov)
    }

    fun isEnabled() = config.enabled && SlayerApi.isInBossFight()
}
