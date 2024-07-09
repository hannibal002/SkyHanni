package at.hannibal2.skyhanni.features.mining.powdertracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.editCopy
import at.hannibal2.skyhanni.utils.CollectionUtils.removeFirst
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PowderChestTimer {

    private val config get() = SkyHanniMod.feature.mining.powderTracker.chestTimer

    private var spawnedChest = mapOf<LorenzVec, SimpleTimeMark>()
    private var display = listOf<Renderable>()

    @SubscribeEvent
    fun onRenderGui(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.position.renderRenderables(display, posLabel = "Powder Chest Timer")
    }

    @SubscribeEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!isEnabled()) return
        val location = event.location
        if (location.distanceToPlayer() > 20) return
        val oldBlock = event.old
        val newBlock = event.new

        if (oldBlock == "air" && newBlock == "chest") {
            spawnedChest = spawnedChest.editCopy {
                put(location, SimpleTimeMark.now().plus(1.minutes))
            }
        }

        if (oldBlock == "chest" && newBlock == "air") {
            spawnedChest = spawnedChest.editCopy {
                remove(location)
            }
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: LorenzTickEvent) {
        if (!isEnabled()) return

        spawnedChest = spawnedChest.editCopy {
            removeFirst { it.value.timeUntil() < 0.seconds }
        }

        display = drawDisplay()
    }

    private fun drawDisplay(): List<Renderable> {
        if (spawnedChest.isEmpty()) return emptyList()

        val list = mutableListOf<Renderable>()
        val count = spawnedChest.size
        val name = StringUtils.pluralize(count, "chest")
        val first = spawnedChest.values.first()
        val timeUntil = first.timeUntil()
        val color = if (timeUntil.inWholeSeconds < 10) "§c" else "§e"

        list.add(Renderable.string("$color$timeUntil §8(§e$count §b$name§8)"))

        return list
    }

    @SubscribeEvent
    fun onRender(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        for ((loc, time) in spawnedChest) {

            val color = when (time.timeUntil().inWholeSeconds) {
                in 0..9 -> LorenzColor.RED
                in 10..29 -> LorenzColor.GOLD
                in 30..60 -> LorenzColor.GREEN
                else -> LorenzColor.WHITE
            }

            event.drawWaypointFilled(loc, color.toColor(), seeThroughBlocks = false)
            val y = if (loc.y <= LocationUtils.playerLocation().y) 1.25 else -0.25
            event.drawString(
                loc.add(y = y, x = 0.5, z = 0.5), "${color.getChatColor()}${time.timeUntil()}",
                seeThroughBlocks = false,
            )
        }
    }

    private fun isEnabled() = config.enabled && IslandType.CRIMSON_ISLE.isInIsland()
}
