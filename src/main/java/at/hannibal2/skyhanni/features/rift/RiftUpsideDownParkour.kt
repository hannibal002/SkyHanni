package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.playerLocation
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.jsonobjects.ParkourJson
import at.hannibal2.skyhanni.utils.jsonobjects.ParkourJson.ShortCut
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

class RiftUpsideDownParkour {
    private var locations = emptyList<LorenzVec>()
    private var shortCuts = emptyList<ShortCut>()
    private var current = -1
    private var visible = false

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ParkourJson>("RiftUpsideDownParkour") ?: return
        locations = data.locations
        shortCuts = data.shortCuts
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (event.message == "§c§lOH NO! THE LAVA OOFED YOU BACK TO THE START!") {
            current = -1
            visible = false
        }
    }

    val lookAhead get() = 2

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        // TODO reenable this
//        if (!RiftAPI.inRift()) return
//        if (LorenzUtils.skyBlockArea != "Mirrorverse") return

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
            }

        }
        for ((index, location) in locations.asSequence().withIndex().drop(current)
            .take(lookAhead) + inProgressVec.map { it.second }) {
            event.drawFilledBoundingBox(axisAlignedBB(location), colorForIndex(index), 1f)
        }
    }

    fun getInProgressPair(): Pair<IndexedValue<LorenzVec>, IndexedValue<LorenzVec>>? {
        if (current < 0 || current + lookAhead >= locations.size) return null
        val currentPosition = locations[current]
        val nextPosition = locations[current + 1]
        val lookAheadStart = locations[current + lookAhead - 1]
        val lookAheadEnd = locations[current + lookAhead]
        if (playerLocation().distance(nextPosition) > currentPosition.distance(nextPosition)) return null
        return Pair(
            IndexedValue(current + lookAhead - 1, lookAheadStart),
            IndexedValue(
                current + lookAhead, lookAheadStart.add(
                    lookAheadEnd.subtract(lookAheadStart)
                        .scale(playerLocation().distance(currentPosition) / currentPosition.distance(nextPosition))
                )
            )
        )
    }

    private fun axisAlignedBB(loc: LorenzVec) = loc.add(-1.0, 0.0, -1.0).boundingToOffset(2, -1, 2).expandBlock()

    private fun colorForIndex(index: Int) = RenderUtils.chromaColor(4.seconds, offset = -index / 12f, brightness = 0.7f)
}

private fun <T : Any> T?.toSingletonListOrEmpty(): List<T> {
    if (this == null) return emptyList()
    return listOf(this)
}
