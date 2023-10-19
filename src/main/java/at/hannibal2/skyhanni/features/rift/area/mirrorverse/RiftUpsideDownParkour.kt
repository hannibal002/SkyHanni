package at.hannibal2.skyhanni.features.rift.area.mirrorverse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ParkourHelper
import at.hannibal2.skyhanni.utils.jsonobjects.ParkourJson
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RiftUpsideDownParkour {
    private val config get() = RiftAPI.config.area.mirrorVerseConfig.upsideDownParkour
    private var parkourHelper: ParkourHelper? = null

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        try {
            val data = event.getConstant<ParkourJson>("RiftUpsideDownParkour") ?: throw Exception()
            parkourHelper = ParkourHelper(
                data.locations.map { it.add(-1.0, -1.0, -1.0) },
                data.shortCuts,
                platformSize = 2.0,
                detectionRange = 2.0
            )
            updateConfig()
            SkyHanniMod.repo.successfulConstants.add("RiftUpsideDownParkour")
        } catch (_: Exception) {
            SkyHanniMod.repo.unsuccessfulConstants.add("RiftUpsideDownParkour")
        }
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!isEnabled()) return
        if (!config.hidePlayers) return

        parkourHelper?.let {
            if (it.inParkour()) {
                event.isCanceled = true
            }
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return

        if (event.message == "§c§lOH NO! THE LAVA OOFED YOU BACK TO THE START!") {
            parkourHelper?.reset()
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        LorenzUtils.onToggle(config.rainbowColor, config.monochromeColor, config.lookAhead) {
            updateConfig()
        }
    }

    private fun updateConfig() {
        parkourHelper?.run {
            rainbowColor = config.rainbowColor.get()
            monochromeColor = config.monochromeColor.get().toChromaColor()
            lookAhead = config.lookAhead.get() + 1
            outline = config.outline
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return

        parkourHelper?.render(event)
    }

    fun isEnabled() = RiftAPI.inRift() && LorenzUtils.skyBlockArea == "Mirrorverse" && config.enabled
}
