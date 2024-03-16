package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.config.features.garden.laneswitch.LaneSwitchNotificationSettings
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.isBarn
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.plots
import at.hannibal2.skyhanni.features.garden.farming.LaneSwitchUtils.canBeEnabled
import at.hannibal2.skyhanni.features.garden.farming.LaneSwitchUtils.enabledContainsPlot
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzUtils.sendTitle
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

class LaneSwitchNotification {

    private val config get() = GardenAPI.config.laneswitch

    private var bps = 0.0 // Blocks per Second
    private var distancesUntilSwitch: List<Double> = listOf()
    private var lastBps = 0.0 // Last blocks per Second
    private var lastPosition = LorenzVec(0, 0, 0)
    private var lastLaneSwitch = SimpleTimeMark.farPast()
    private var lastWarning = SimpleTimeMark.farPast()
    private var lastDistancesUntilSwitch: List<Double> = listOf()
    private var lastDistance = 0.0

    companion object {
        private val config get() = GardenAPI.config.laneswitch

        @JvmStatic
        fun playUserSound() {
            SoundUtils.createSound(
                config.notification.sound.notificationSound,
                config.notification.sound.notificationPitch,
            ).playSound()
        }
    }

    private fun switchPossibleInTime(from: LorenzVec, to: LorenzVec, speed: Double, time: Int): Boolean {
        return from.distance(to) <= speed * time
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        val settings = config.notification.settings
        val plot = GardenPlotAPI.getCurrentPlot() ?: return
        if (!plot.unlocked) return

        val plotIndex = plots.indexOf(plot)
        val position = LocationUtils.playerLocation()
        val farmEnd = LaneSwitchUtils.getFarmBounds(plotIndex, position, lastPosition, enabledPlots) ?: return
        lastPosition = position
        bps = LocationUtils.distanceFromPreviousTick()
        distancesUntilSwitch = farmEnd.map { end -> end.distance(position).round(1) }

        testForLaneSwitch(settings, farmEnd, position)
        lastBps = bps
    }

    private fun testForLaneSwitch(
        settings: LaneSwitchNotificationSettings,
        farmEnd: List<LorenzVec>,
        position: LorenzVec,
    ) {
        val farmLength = farmEnd[0].distance(farmEnd[1])
        // farmLength / bps to get the time needed to travel the distance, - the threshold times the farm length divided by the length of 2 plots (to give some room)
        val threshold = settings.threshold
        // TODO find a name for this variable
        val FIND_A_NAME_FOR_ME = threshold * (farmLength / 480)
        val farmTraverseTime = ((farmLength / bps) - FIND_A_NAME_FOR_ME).seconds
        val bpsDifference = (bps - lastBps).absoluteValue

        if (farmEnd.isEmpty() || lastLaneSwitch.passedSince() < farmTraverseTime || bpsDifference > 20) return
        if (!farmEnd.any { switchPossibleInTime(position, it, bps, threshold) }) return

        with(settings) {
            sendTitle(color.getChatColor() + text, duration.seconds)
        }
        playUserSound()
        lastLaneSwitch = SimpleTimeMark.now()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled() || !config.distanceUntilSwitch) return
        if (distancesUntilSwitch.isEmpty()) return
        if (lastDistancesUntilSwitch.isEmpty()) {
            lastDistancesUntilSwitch = distancesUntilSwitch
        }

        val distances = listOf(
            distancesUntilSwitch[0] - lastDistancesUntilSwitch[0],
            distancesUntilSwitch[1] - lastDistancesUntilSwitch[1]
        ) //Get changes in the distances
        val distance = if (distances.all { it != 0.0 }) {
            if (distances[0] > 0) distancesUntilSwitch[1] else distancesUntilSwitch[0] // get the direction the player is traveling and get the distance to display from that
        } else {
            lastDistance // display last value if no change is detected
        }

        config.distanceUntilSwitchPos.renderString("Distance until Switch: $distance", posLabel = "Movement Speed")
        lastDistancesUntilSwitch = distancesUntilSwitch
        lastDistance = distance
    }

    private fun plotsLoaded(): Boolean {
        if (plots.any { it.unlocked }) return true

        if (lastWarning.passedSince() >= 30.seconds) {
            ChatUtils.clickableChat("§eOpen your configure plots for lane switch detection to work.", "/desk")
            lastWarning = SimpleTimeMark.now()
        }
        return false
    }

    private fun isEnabled() = GardenAPI.isCurrentlyFarming() && config.enabled && plotsLoaded()

    public var enabledPlots: MutableList<Int> = mutableListOf()
    private var enableEditing = false

    private val grayDyeItem = ItemStack(Items.dye, 1, EnumDyeColor.GRAY.dyeDamage)
    private val limeDyeItem = ItemStack(Items.dye, 1, EnumDyeColor.LIME.dyeDamage)
    private val barrierItem = ItemStack(Blocks.barrier, 1)
    private val resetItem = ItemStack(Blocks.redstone_block, 1)
    private var ironPickaxeItem = ItemStack(Items.iron_pickaxe, 1)

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!isMenuEnabled()) return

        if (event.inventory is ContainerLocalMenu) {
            if (event.slotNumber == 50) event.replaceWith(ironPickaxeItem)
            if (!enableEditing) return
            if (event.slotNumber == 8) event.replaceWith(resetItem)

            val plot = plots.find { it.inventorySlot == event.slotNumber } ?: return
            val plotIndex = plots.indexOf(plot)
            if (!plot.unlocked || plot.isBarn()) {
                event.replaceWith(barrierItem)
                return
            }
            if (!enabledPlots.contains(plotIndex)) {
                event.replaceWith(grayDyeItem)
            } else {
                event.replaceWith(limeDyeItem)
            }
        }
    }

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!isMenuEnabled()) return

        val list = event.toolTip
        val index = event.slot.slotNumber
        if (index == 50) {
            list.clear()
            list.add(if (enableEditing) "§aConfigure Lane Switch Plots" else "§cConfigure Lane Switch Plots")
        }
        if (!enableEditing) return
        if (index == 8) {
            list.clear()
            list.add("§cClick to reset Config")
        }
        val plot = plots.find { it.inventorySlot == event.slot.slotNumber } ?: return
        list.clear()
        if (!plot.unlocked) {
            list.add("§cPlot not unlocked")
            return
        } else if (plot.isBarn()) {
            list.add("§cBarn")
            return
        }
        list.add(if (enabledContainsPlot(plot, enabledPlots)) "§cClick to disable Plot for Lane Switch" else "§aClick to enable Plot for Lane Switch")
    }

    @SubscribeEvent
    fun onStackClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isMenuEnabled()) return
        if (event.slotId == 50) {
            event.isCanceled = true
            enableEditing = if (enableEditing) false else true
        }
        if (!enableEditing) return
        if (event.slotId == 8) {
            event.isCanceled = true
            enabledPlots.clear()
        }
        val plot = plots.find { it.inventorySlot == event.slotId } ?: return
        val plotIndex = plots.indexOf(plot)
        event.isCanceled = true
        if (!plot.unlocked || plot.isBarn()) return
        if (enabledContainsPlot(plot, enabledPlots)) {
            enabledPlots.remove(plotIndex)
        } else {
            if (!canBeEnabled(plotIndex, enabledPlots)) return
            enabledPlots.add(plotIndex)
        }
    }

    fun isMenuEnabled() =
        GardenAPI.inGarden() && (InventoryUtils.openInventoryName() == "Configure Plots" || InventoryUtils.openInventoryName() == "Configure Lane Switch Plots") && config.enabled
}
