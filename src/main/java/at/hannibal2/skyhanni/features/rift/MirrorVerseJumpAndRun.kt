package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.jsonobjects.RiftMirrorJumpJson
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

class MirrorVerseJumpAndRun {
    private var locations = listOf<LorenzVec>()
    private var current = -1
    private var visible = false

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<RiftMirrorJumpJson>("MirrorVerseJump") ?: return
        locations = data.locations
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (event.message == "§c§lOH NO! THE LAVA OOFED YOU BACK TO THE START!") {
            current = -1
            visible = false
        }
    }

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

        // TODO add config
//        val platformsLookAhead = 2
        val lookAhead = 2

        for ((index, location) in locations.withIndex()) {
            if (location.distanceToPlayer() < 2) {
                if (Minecraft.getMinecraft().thePlayer.onGround) {
                    current = index
                }
            }
        }

        for ((index, location) in locations.withIndex()) {
            if (index < current) continue
            if (index > current + lookAhead) break

            event.drawFilledBoundingBox(axisAlignedBB(location), colorForIndex(index), 1f)

            locations.getOrNull(index + 1)?.let { next ->
                if (current + lookAhead != index)
                    event.draw3DLine(
                        location,
                        next,
                        colorForIndex(index),
                        5,
                        false,
                        colorForIndex(index + 1)
                    )
            }

            if (current == index) {
                renderLookAhead(index, lookAhead, location, event)
            }

            // TODO add shortcuts to repo
            if (index == 14) {
                locations[25].let {
                    event.draw3DLine(location, it, Color.RED, 3, false)
                    event.drawFilledBoundingBox(axisAlignedBB(it), Color.RED, 1f)
                    event.drawDynamicText(it.add(-0.5, 1.0, -0.5), "§cShortcut", 2.5)
                }
            }
        }
    }

    private fun renderLookAhead(
        index: Int,
        lookAhead: Int,
        location: LorenzVec,
        event: RenderWorldLastEvent
    ) {
        locations.getOrNull(index + 1)?.let { next ->
            val currentDistance = next.distanceToPlayer()
            val nextDistance = next.distance(location)
            val percentage = 1 - (currentDistance / nextDistance)
            if (percentage > 0) {
                locations.getOrNull(index + lookAhead + 1)?.let { b ->
                    val a = locations[index + lookAhead]
                    val direction = b.subtract(a)
                    val middle = a.add(direction.multiply(percentage))

                    event.drawFilledBoundingBox(
                        axisAlignedBB(middle),
                        colorForIndex(index + lookAhead),
                        0.7f
                    )

                    event.draw3DLine(
                        a,
                        middle,
                        colorForIndex(index + lookAhead),
                        5,
                        false,
                        targetColor = colorForIndex(index + lookAhead + 1)
                    )
                }
            }
        }
    }

    private fun axisAlignedBB(loc: LorenzVec) = loc.add(-1.0, 0.0, -1.0).boundingToOffset(2, -1, 2).expandBlock()

    private fun colorForIndex(index: Int) = RenderUtils.chromaColor(4.seconds, offset = -index / 12f, brightness = 0.7f)
}
