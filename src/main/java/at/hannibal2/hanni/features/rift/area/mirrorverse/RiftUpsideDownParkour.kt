package at.hannibal2.hanni.features.rift.area.mirrorverse

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.jsonobjects.repo.ParkourJson
import at.hannibal2.hanni.events.CheckRenderEntityEvent
import at.hannibal2.hanni.events.ConfigLoadEvent
import at.hannibal2.hanni.events.RepositoryReloadEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ConditionalUtils
import at.hannibal2.hanni.utils.ParkourHelper
import net.minecraft.entity.Entity

@HanniModule
object RiftUpsideDownParkour {

    private val config get() = RiftApi.config.area.mirrorverse.upsideDownParkour
    private var parkourHelper: ParkourHelper? = null

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ParkourJson>("RiftUpsideDownParkour")
        parkourHelper = ParkourHelper(
            data.locations.map { it.add(-1.0, -1.0, -1.0) }, // TODO remove offset. change repo instead
            data.shortCuts,
            platformSize = 2.0,
            detectionRange = 2.0,
        )
        updateConfig()
    }

    @HandleEvent
    fun onCheckRender(event: CheckRenderEntityEvent<Entity>) {
        if (!isEnabled()) return
        if (!config.hidePlayers) return

        parkourHelper?.let {
            if (it.inParkour()) {
                event.cancel()
            }
        }
    }

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return

        if (event.message == "§c§lOH NO! THE LAVA OOFED YOU BACK TO THE START!") {
            parkourHelper?.reset()
        }
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.rainbowColor, config.monochromeColor, config.lookAhead) {
            updateConfig()
        }
    }

    // todo this code is duplicated across a lot of Rift
    //  make an extensible config/helper pair that all of these classes can use
    private fun updateConfig() {
        parkourHelper?.run {
            rainbowColor = config.rainbowColor.get()
            monochromeColor = config.monochromeColor.get()
            lookAhead = config.lookAhead.get() + 1
            outline = config.outline
        }
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return

        parkourHelper?.render(event)
    }

    fun isEnabled() = RiftApi.inRift() && RiftApi.inMirrorVerse && config.enabled
}
