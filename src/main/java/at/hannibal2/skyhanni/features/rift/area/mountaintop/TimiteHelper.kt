package at.hannibal2.skyhanni.features.rift.area.mountaintop

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.getBlockColor
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.isStainedGlassPane
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TimiteHelper {

    private val config get() = SkyHanniMod.feature.rift.area.mountaintop.timite
    private val TIME_GUN = "TIME_GUN".toInternalName()

    // TODO reform to data class, use Resettable
    private var holdingClick = SimpleTimeMark.farPast()
    private var lastClick = SimpleTimeMark.farPast()
    private var currentPos: LorenzVec? = null
    private var currentBlockState: BlockState? = null
    private var doubleTimeShooting = false

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onBlockHit(event: BlockClickEvent) {
        if (!isEnabled() || !config.evolutionTimer) return
        if (InventoryUtils.itemInHandId != TIME_GUN) return
        if (event.clickType != ClickType.RIGHT_CLICK) return
        if (event.position != currentPos || currentBlockState != event.blockState) {
            lastClick = SimpleTimeMark.farPast()

            if (event.position == currentPos && currentBlockState != event.blockState) {
                locations[event.position] = SimpleTimeMark.now()
                doubleTimeShooting = true
            } else {
                doubleTimeShooting = false
            }
        }
        currentPos = event.position
        currentBlockState = event.blockState

        val blockState = event.blockState
        if (!blockState.isStainedGlassPane(ColoredBlockCompat.BLUE) && !blockState.isStainedGlassPane(ColoredBlockCompat.LIGHT_BLUE)) return

        if (lastClick + 300.milliseconds > SimpleTimeMark.now()) {
            lastClick = SimpleTimeMark.now()
            return
        }
        lastClick = SimpleTimeMark.now()
        holdingClick = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onGuiRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled() || !config.evolutionTimer) return
        if (InventoryUtils.itemInHandId != TIME_GUN) return
        if (lastClick + 400.milliseconds < SimpleTimeMark.now()) {
            holdingClick = SimpleTimeMark.farPast()
            doubleTimeShooting = false
        }
        if (holdingClick.isFarPast()) return

        if (currentBlockState?.isStainedGlassPane() != true) return

        val time = if (doubleTimeShooting) 1800 else 2000
        val timeLeft = holdingClick + time.milliseconds
        if (!timeLeft.isInPast()) {
            val formattedTime = timeLeft.timeUntil().format(showMilliSeconds = true)
            config.timerPosition.renderRenderable(
                Renderable.text("§b$formattedTime"),
                posLabel = "Timite Helper",
            )
        }
    }

    private val locations = mutableMapOf<LorenzVec, SimpleTimeMark>()

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled() || !config.expiryTimer) return

        val map = BlockUtils.nearbyBlocks(
            LocationUtils.playerLocation(),
            distance = 15,
            condition = { it.isStainedGlassPane() },
        )

        for ((loc, state) in map) {
            val color = state.getBlockColor()
            if (color != LorenzColor.BLUE && color != LorenzColor.AQUA) continue
            if (locations[loc] == null) locations[loc] = SimpleTimeMark.now()
        }

        locations.entries.removeIf { (location, _) ->
            val state = location.getBlockStateAt()
            state.block == Blocks.AIR || state.isStainedGlassPane(ColoredBlockCompat.LIGHT_BLUE)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled() || !config.expiryTimer) return

        for (location in locations.entries) {
            val timeLeft = location.value + 31.seconds
            if (timeLeft.timeUntil() < 6.seconds) {
                event.drawDynamicText(location.key, "§c${timeLeft.timeUntil().format()}", 1.5)
            }
        }
    }

    @HandleEvent
    fun onWorldChange() = locations.clear()

    private fun isEnabled() = RiftApi.inMountainTop() && config.enabled
}
