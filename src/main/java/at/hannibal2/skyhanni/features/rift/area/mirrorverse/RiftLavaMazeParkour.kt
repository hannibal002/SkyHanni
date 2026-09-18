package at.hannibal2.skyhanni.features.rift.area.mirrorverse

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.ParkourJson
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ParkourHelper
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.Entity

@SkyHanniModule
object RiftLavaMazeParkour {

    private val config get() = RiftApi.config.area.mirrorverse.lavaMazeConfig
    private var parkourHelper: ParkourHelper? = null

    /**
     * REGEX-TEST: EEK! THE LAVA OOFED YOU!
     */
    private val failLaveMazePattern by RepoPattern.pattern(
        "rift.lava-maze-parkour.fail",
        "EEK! THE LAVA OOFED YOU!"
    )

    @HandleEvent
    private fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ParkourJson>("RiftLavaMazeParkour")
        parkourHelper = ParkourHelper(
            data.locations,
            data.shortCuts,
            platformSize = 1.0,
            detectionRange = 1.0
        )
        updateConfig()
    }

    @HandleEvent
    private fun onCheckRender(event: CheckRenderEntityEvent<Entity>) {
        if (!isEnabled()) return
        if (!config.hidePlayers) return

        parkourHelper?.let {
            if (it.inParkour()) {
                event.cancel()
            }
        }
    }

    @HandleEvent
    private fun onChat(event: SystemMessageEvent.Allow) {
        if (!isEnabled()) return

        if (failLaveMazePattern.matches(event.cleanMessage)) {
            parkourHelper?.reset()
        }
    }

    @HandleEvent
    private fun onConfigLoad() {
        ConditionalUtils.onToggle(config.rainbowColor, config.monochromeColor, config.lookAhead) {
            updateConfig()
        }
    }

    private fun updateConfig() {
        parkourHelper?.run {
            rainbowColor = config.rainbowColor.get()
            monochromeColor = config.monochromeColor.get()
            lookAhead = config.lookAhead.get() + 1
        }
    }

    @HandleEvent
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        parkourHelper?.render(event)
    }

    fun isEnabled() = RiftApi.inRift() && RiftApi.inMirrorVerse && config.enabled
}
