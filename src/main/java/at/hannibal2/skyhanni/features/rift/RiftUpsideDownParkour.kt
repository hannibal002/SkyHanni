package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.toChromaColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.RenderUtils.outlineTopFace
import at.hannibal2.skyhanni.utils.ParkourHelper
import at.hannibal2.skyhanni.utils.jsonobjects.ParkourJson
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RiftUpsideDownParkour {
    private val config get() = SkyHanniMod.feature.rift.mirrorVerse.upsideDownParkour

    private var parkourHelper: ParkourHelper? = null

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ParkourJson>("RiftUpsideDownParkour") ?: return
        parkourHelper = ParkourHelper(data.locations, data.shortCuts)
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
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!isEnabled()) return

        if (current == locations.size - 1) visible = false

        val distanceToPlayer = locations.first().distanceToPlayer()
        if (distanceToPlayer < 2) {
            visible = true
        } else if (distanceToPlayer > 15) {
            if (current < 1) {
                visible = false
            }
        }

        if (!visible) return

        for ((index, location) in locations.withIndex()) {
            if (location.distanceToPlayer() < 2) {
                if (Minecraft.getMinecraft().thePlayer.onGround) {
                    current = index
                }
            }
        }
        if (current < 0) return

        val inProgressVec = getInProgressPair().toSingletonListOrEmpty()
        for ((prev, next) in locations.asSequence().withIndex().zipWithNext().drop(current)
            .take(lookAhead - 1) + inProgressVec) {
            event.draw3DLine(prev.value, next.value, colorForIndex(prev.index), 5, false, colorForIndex(next.index))
        }
        val nextShortcuts = current until current + lookAhead
        for (shortCut in shortCuts) {
            if (shortCut.from in nextShortcuts && shortCut.to in locations.indices) {
                event.draw3DLine(locations[shortCut.from], locations[shortCut.to], Color.RED, 3, false)
                event.drawFilledBoundingBox(axisAlignedBB(locations[shortCut.to]), Color.RED, 1f)
                event.drawDynamicText(locations[shortCut.to].add(-0.5, 1.0, -0.5), "§cShortcut", 2.5)
                if (config.outline) event.outlineTopFace(axisAlignedBB(locations[shortCut.to]), 2, Color.BLACK, true)
            }
        }
        for ((index, location) in locations.asSequence().withIndex().drop(current)
            .take(lookAhead) + inProgressVec.map { it.second }) {
            if (config.outline && location in locations) {
                event.drawFilledBoundingBox(axisAlignedBB(location), colorForIndex(index), 1f)
                if (config.outline && location in locations) event.outlineTopFace(axisAlignedBB(location), 2, Color.BLACK, true)
            }
            event.drawFilledBoundingBox(axisAlignedBB(location), colorForIndex(index), .5f)
        }
    }
    
    @SubscribeEvent     
    fun onConfigLoad(event: ConfigLoadEvent) {
        LorenzUtils.onToggle(config.rainbowColor, config.monochromeColor, config.lookAhead) {
            parkourHelper?.run {
                rainbowColor = config.rainbowColor.get()
                monochromeColor = config.monochromeColor.get().toChromaColor()
                lookAhead = config.lookAhead.get() + 1
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!isEnabled()) return

        parkourHelper?.render(event)
    }

    fun isEnabled() = RiftAPI.inRift() && LorenzUtils.skyBlockArea == "Mirrorverse" && config.enabled
}
