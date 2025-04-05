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
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraft.block.BlockStainedGlassPane
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TimiteHelper {

    private val TIME_GUN = "TIME_GUN".toInternalName()
    private var holdingClick = SimpleTimeMark.farPast()
    private var lastClick = SimpleTimeMark.farPast()
    private val config get() = SkyHanniMod.feature.rift.area.mountaintop.timite
    private var currentPos: LorenzVec? = null
    private var currentBlockState: IBlockState? = null
    private var doubleTimeShooting = false

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onBlockHit(event: BlockClickEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.itemInHandId != TIME_GUN) return
        if (event.clickType != ClickType.RIGHT_CLICK) return
        if (event.position != currentPos || currentBlockState != event.getBlockState) {
            lastClick = SimpleTimeMark.farPast()

            if (event.position == currentPos && currentBlockState != event.getBlockState) {
                locations[event.position] = SimpleTimeMark.now()
                doubleTimeShooting = true
            } else {
                doubleTimeShooting = false
            }
        }
        currentPos = event.position
        currentBlockState = event.getBlockState
        if (event.getBlockState.block != Blocks.stained_glass_pane) return
        val color = event.getBlockState.getValue(BlockStainedGlassPane.COLOR)
        if (color != EnumDyeColor.BLUE && color != EnumDyeColor.LIGHT_BLUE) return
        if (lastClick + 300.milliseconds > SimpleTimeMark.now()) {
            lastClick = SimpleTimeMark.now()
            return
        }
        lastClick = SimpleTimeMark.now()
        holdingClick = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onGuiRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.itemInHandId != TIME_GUN) return
        if (lastClick + 400.milliseconds < SimpleTimeMark.now()) {
            holdingClick = SimpleTimeMark.farPast()
            doubleTimeShooting = false
        }
        if (holdingClick.isFarPast()) return

        if ((currentBlockState?.block ?: return) != Blocks.stained_glass_pane) return
        // this works for me but idk if ive just tuned it for my ping only
        val time = if (doubleTimeShooting) 1800 else 2000
        val timeLeft = holdingClick + time.milliseconds
        if (!timeLeft.isInPast()) {
            val formattedTime = timeLeft.timeUntil().format(showMilliSeconds = true)
            config.timerPosition.renderString("§b$formattedTime", 0, 0, "Timite Helper")
        }
    }

    private val locations = mutableMapOf<LorenzVec, SimpleTimeMark>()

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!RiftApi.inMountainTop() || !config.expiryTimer) return

        val map = BlockUtils.nearbyBlocks(
            LocationUtils.playerLocation(),
            distance = 15,
            filter = Blocks.stained_glass_pane,
        )

        for ((loc, state) in map) {
            val color = state.getValue(BlockStainedGlassPane.COLOR)
            if (color != EnumDyeColor.BLUE && color != EnumDyeColor.LIGHT_BLUE) continue
            if (locations[loc] == null) locations[loc] = SimpleTimeMark.now()
        }

        val iterator = locations.entries.iterator()
        while (iterator.hasNext()) {
            val state = iterator.next().key.getBlockStateAt()
            if (state.block == Blocks.air) {
                iterator.remove()
            } else if (state.block == Blocks.stained_glass_pane) {
                val color = state.getValue(BlockStainedGlassPane.COLOR)
                if (color == EnumDyeColor.LIGHT_BLUE) {
                    iterator.remove()
                }
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!RiftApi.inMountainTop() || !config.expiryTimer) return

        for (location in locations.entries) {
            val timeLeft = location.value + 31.seconds
            if (timeLeft.timeUntil() < 6.seconds) {
                event.drawDynamicText(location.key, "§c${timeLeft.timeUntil().format()}", 1.5)
            }
        }
    }

    @HandleEvent
    fun onWorldChange() {
        locations.clear()
    }

    private fun isEnabled() = RiftApi.inMountainTop() && config.evolutionTimer
}
