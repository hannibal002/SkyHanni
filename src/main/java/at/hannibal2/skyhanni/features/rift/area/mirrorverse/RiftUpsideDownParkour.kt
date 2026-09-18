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
object RiftUpsideDownParkour {

    private val config get() = RiftApi.config.area.mirrorverse.upsideDownParkour
    private var parkourHelper: ParkourHelper? = null

    /**
     * REGEX-TEST: OH NO! THE LAVA OOFED YOU BACK TO THE START!
     */
    private val failParkourMessagePattern by RepoPattern.pattern(
        "rift.upside-down-parkour.fail",
        "OH NO! THE LAVA OOFED YOU BACK TO THE START!",
    )

    @HandleEvent
    private fun onRepoReload(event: RepositoryReloadEvent) {
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

        if (failParkourMessagePattern.matches(event.cleanMessage)) {
            parkourHelper?.reset()
        }
    }

    @HandleEvent
    private fun onConfigLoad() {
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
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        parkourHelper?.render(event)
    }

    fun isEnabled() = RiftApi.inRift() && RiftApi.inMirrorVerse && config.enabled
}
