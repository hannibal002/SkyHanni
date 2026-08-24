package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.slayer.SlayerStateChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SoundUtils

@SkyHanniModule
object SlayerCocoonWarning {

    private val config get() = SlayerApi.config

    @HandleEvent
    private fun onSlayerStateChange(event: SlayerStateChangeEvent) {
        if (event.state != COCOONED) return
        if (config.cocoonTitle) TitleManager.sendTitle("§lSlayer Boss Cocooned!")
        if (config.cocoonDing) SoundUtils.repeatSound(100, 10, SoundUtils.plingSound)
    }
}
