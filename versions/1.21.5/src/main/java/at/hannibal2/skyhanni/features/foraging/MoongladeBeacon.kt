package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.events.minecraft.ServerTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.isEnchanted
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.ModernPatterns
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import kotlin.math.abs

@SkyHanniModule
object MoongladeBeacon {

    private val config get() = SkyHanniMod.feature.foraging.moongladeBeacon

    private val colorOrder = listOf(
        Items.WHITE_STAINED_GLASS_PANE,
        Items.ORANGE_STAINED_GLASS_PANE,
        Items.MAGENTA_STAINED_GLASS_PANE,
        Items.LIGHT_BLUE_STAINED_GLASS_PANE,
        Items.YELLOW_STAINED_GLASS_PANE,
        Items.LIME_STAINED_GLASS_PANE,
        Items.PINK_STAINED_GLASS_PANE,
        Items.CYAN_STAINED_GLASS_PANE,
        Items.PURPLE_STAINED_GLASS_PANE,
        Items.BLUE_STAINED_GLASS_PANE,
        Items.BROWN_STAINED_GLASS_PANE,
        Items.GREEN_STAINED_GLASS_PANE,
        Items.RED_STAINED_GLASS_PANE,
    )

    private val colorOrderNames = listOf(
        "§fWhite",
        "§6Orange",
        "§dMagenta",
        "§bLight Blue",
        "§eYellow",
        "§aLime",
        "§dPink",
        "§3Cyan",
        "§5Purple",
        "§9Blue",
        "§6Brown",
        "§2Green",
        "§cRed",
    )

    private val pitchLevels = listOf(
        "Low",
        "Normal",
        "High",
    )

    private val speedMap = mapOf(
        12 to 5,
        22 to 4,
        32 to 3,
        42 to 2,
        52 to 1,
    )

    private val pitchMap = mapOf(
        0.0952381f to 0,
        0.7936508f to 1,
        1.4920635f to 2,
    )

    private const val COLOR_SELECT_SLOT = 46
    private const val SPEED_SELECT_SLOT = 48
    private const val PITCH_SELECT_SLOT = 50
    private const val PAUSE_SELECT_SLOT = 52

    private val MATCH_SLOTS = 10..16
    private val CHANGE_SLOTS = 28..34

    private var inInventory = false
    private var upgradingStrength = false

    private var normalTuning = TuneData()
    private var enchantedTuning = TuneData(isEnchanted = true)

    private var display = emptyList<Renderable>()

    private fun solverEnabled(): Boolean = inInventory && config.enabled

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        // todo doesnt work in the upgrading menu atm
        inInventory = event.inventoryName == "Tune Frequency" // || event.inventoryName == "Upgrade Signal Strength"
        if (!inInventory) return
        if (event.inventoryName == "Upgrade Signal Strength") {
            upgradingStrength = true
        }
        currentServerTicks = 0
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (!inInventory) return
        inInventory = false
        normalTuning.clear()
        enchantedTuning.clear()
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!inInventory) return
        if (!config.useMiddleClick) return

        if (event.clickedButton != 0) return
        event.makePickblock()
    }

    private var currentServerTicks = 0

    @HandleEvent
    fun onServerTick(event: ServerTickEvent) {
        if (!inInventory) return
        currentServerTicks++
    }

    @HandleEvent
    fun onPlaySound(event: PlaySoundEvent) {
        if (!inInventory) return
        if (event.soundName != "note.bassattack") return
        val pitch = pitchMap[event.pitch] ?: return
        if (upgradingStrength) return
        if (normalTuning.targetPitch == null) return
        if (normalTuning.currentPitch == pitch) return
        normalTuning.targetPitch = pitch
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!solverEnabled()) return

        config.displayPosition.renderRenderables(display, posLabel = "Moonglade Beacon")
    }

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!solverEnabled()) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            when (slot.index) {
                normalTuning.colorSelectSlot -> {
                    if (normalTuning.getColorOffset() != 0) continue
                    slot.highlight(LorenzColor.GREEN.addOpacity(200))
                }
                normalTuning.speedSelectSlot -> {
                    if (normalTuning.getSpeedOffset() != 0) continue
                    slot.highlight(LorenzColor.GREEN.addOpacity(200))
                }
                normalTuning.pitchSelectSlot -> {
                    if (normalTuning.getPitchOffset() != 0) continue
                    slot.highlight(LorenzColor.GREEN.addOpacity(200))
                }
                enchantedTuning.colorSelectSlot -> {
                    if (!upgradingStrength || enchantedTuning.getColorOffset() != 0) continue
                    slot.highlight(LorenzColor.GREEN.addOpacity(200))
                }
                enchantedTuning.speedSelectSlot -> {
                    if (!upgradingStrength || enchantedTuning.getSpeedOffset() != 0) continue
                    slot.highlight(LorenzColor.GREEN.addOpacity(200))
                }
                enchantedTuning.pitchSelectSlot -> {
                    if (!upgradingStrength || enchantedTuning.getPitchOffset() != 0) continue
                    slot.highlight(LorenzColor.GREEN.addOpacity(200))
                }
            }
        }
    }

    @HandleEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!solverEnabled()) return
        when (event.slot.index) {
            normalTuning.colorSelectSlot -> {
                event.labelIfAble(normalTuning.getColorOffset())
            }

            normalTuning.speedSelectSlot -> {
                event.labelIfAble(normalTuning.getSpeedOffset())
            }

            normalTuning.pitchSelectSlot -> {
                event.labelIfAble(normalTuning.getPitchOffset())
            }

            enchantedTuning.colorSelectSlot -> {
                if (upgradingStrength) event.labelIfAble(enchantedTuning.getColorOffset())
            }

            enchantedTuning.speedSelectSlot -> {
                if (upgradingStrength) event.labelIfAble(enchantedTuning.getSpeedOffset())
            }

            enchantedTuning.pitchSelectSlot -> {
                if (upgradingStrength) event.labelIfAble(enchantedTuning.getPitchOffset())
            }
        }
    }

    private fun RenderInventoryItemTipEvent.labelIfAble(label: Int) {
        if (label <= 0) return
        stackTip = "§a$label"
    }

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!inInventory) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            when (slot.index) {
                in MATCH_SLOTS -> {
                    val colorIndex = slot.stack.getColorIndex()
                    if (colorIndex == -1) continue
                    val isItemEnchanted = slot.stack.hasEnchantments()
                    val tuningData = if (isItemEnchanted) enchantedTuning else normalTuning
                    tuningData.targetColor = colorIndex
                    tuningData.updateMatchSlot(slot.index)
                }

                in CHANGE_SLOTS -> {
                    val colorIndex = slot.stack.getColorIndex()
                    if (colorIndex == -1) continue
                    val isItemEnchanted = slot.stack.hasEnchantments()
                    val tuningData = if (isItemEnchanted) enchantedTuning else normalTuning
                    tuningData.currentColor = colorIndex
                }

                normalTuning.colorSelectSlot -> {
                    val color = slot.stack.getColorFromItem() ?: continue
                    normalTuning.currentColor = color
                }

                enchantedTuning.colorSelectSlot -> {
                    if (!upgradingStrength) continue
                    val color = slot.stack.getColorFromItem() ?: continue
                    enchantedTuning.currentColor = color
                }

                normalTuning.speedSelectSlot -> {
                    val speed = slot.stack.getSpeedFromItem() ?: continue
                    normalTuning.currentSpeed = speed
                }

                enchantedTuning.speedSelectSlot -> {
                    if (!upgradingStrength) continue
                    val speed = slot.stack.getSpeedFromItem() ?: continue
                    enchantedTuning.currentSpeed = speed
                }

                normalTuning.pitchSelectSlot -> {
                    val pitch = slot.stack.getPitchFromItem() ?: continue
                    normalTuning.currentPitch = pitch
                    if (normalTuning.targetPitch == null) normalTuning.targetPitch = pitch
                }

                enchantedTuning.pitchSelectSlot -> {
                    if (!upgradingStrength) continue
                    val pitch = slot.stack.getPitchFromItem() ?: continue
                    enchantedTuning.currentPitch = pitch
                    if (enchantedTuning.targetPitch == null) enchantedTuning.targetPitch = pitch
                }

                normalTuning.pauseSelectSlot -> {
                    normalTuning.isPaused = slot.stack.isPaused()
                }

                enchantedTuning.pauseSelectSlot -> {
                    if (!upgradingStrength) continue
                    enchantedTuning.isPaused = slot.stack.isPaused()
                }
            }
        }
        updateDisplay()
    }

    private fun updateDisplay() {
        val newList = mutableListOf<Renderable>()

        newList.add(StringRenderable("§d§lMoonglade Beacon Solver"))
        newList.add(StringRenderable("§7Target Color: ${formatTargetColor(normalTuning.targetColor)}"))
        newList.add(StringRenderable("§7Target Speed: §a${formatTargetSpeed(normalTuning.targetSpeed)}"))
        newList.add(StringRenderable("§7Target Pitch: §a${formatTargetPitch(normalTuning.targetPitch)}"))

        if (upgradingStrength) {
            newList.add(StringRenderable(""))
            newList.add(StringRenderable("§aEnchanted Tuning"))
            newList.add(StringRenderable("§7Target Color: ${formatTargetColor(enchantedTuning.targetColor)}"))
            newList.add(StringRenderable("§7Target Speed: §a${formatTargetSpeed(enchantedTuning.targetSpeed)}"))
            newList.add(StringRenderable("§7Target Pitch: §a${formatTargetPitch(enchantedTuning.targetPitch)}"))
        }

        display = newList
    }

    private fun formatTargetColor(color: Int?): String {
        if (color == null) return "§eUnknown"
        return colorOrderNames.getOrNull(color) ?: "§eUnknown"
    }

    private fun formatTargetSpeed(speed: Int?): String {
        if (speed == null) return "§eCalculating.."
        return speed.toString()
    }

    private fun formatTargetPitch(pitch: Int?): String {
        if (upgradingStrength) {
            return "§ePitch unavailable, try randomly clicking"
        }
        if (pitch == null) return "§eUnknown"
        return pitchLevels.getOrNull(pitch) ?: "§eUnknown"
    }

    private fun ItemStack.getColorIndex(): Int {
        return colorOrder.indexOf(this.item)
    }

    private fun ItemStack.getColorFromItem(): Int? {
        ModernPatterns.beaconCurrentColorPattern.firstMatcher(getLore()) {
            val colorName = group("color")
            return colorOrderNames.indexOf(colorName).takeIf { it >= 0 }
        }
        return null
    }

    private fun ItemStack.getSpeedFromItem(): Int? {
        ModernPatterns.beaconCurrentSpeedPattern.firstMatcher(getLore()) {
            val speed = group("speed")?.formatIntOrNull() ?: return@firstMatcher null
            return speed
        }
        return null
    }

    private fun ItemStack.getPitchFromItem(): Int? {
        ModernPatterns.beaconCurrentPitchPattern.firstMatcher(getLore()) {
            val pitchName = group("pitch") ?: return@firstMatcher null
            return pitchLevels.indexOf(pitchName).takeIf { it >= 0 }
        }
        return null
    }

    private fun ItemStack.isPaused(): Boolean {
        return this.item == Items.RED_TERRACOTTA
    }

    private data class TuneData(
        val isEnchanted: Boolean = false,
    ) {
        var targetColor: Int? = null
        var targetSpeed: Int? = null
        var targetPitch: Int? = null
        var isPaused: Boolean = false
        var currentColor: Int? = null
        var currentSpeed: Int? = null
        var currentPitch: Int? = null

        private var lastServerTickCount = 0
        private var currentMatchSlot: Int = MATCH_SLOTS.first

        private var recentTicks: MutableList<Int> = mutableListOf()

        val slotOffset = if (upgradingStrength && !isEnchanted) -9 else 0
        val colorSelectSlot = COLOR_SELECT_SLOT + slotOffset
        val speedSelectSlot = SPEED_SELECT_SLOT + slotOffset
        val pitchSelectSlot = PITCH_SELECT_SLOT + slotOffset
        val pauseSelectSlot = PAUSE_SELECT_SLOT + slotOffset

        fun updateMatchSlot(slot: Int) {
            if (slot == currentMatchSlot) return
            currentMatchSlot = slot
            val tickDifference = currentServerTicks - lastServerTickCount
            if (tickDifference == 0) return
            recentTicks.add(tickDifference)
            lastServerTickCount = currentServerTicks
            if (upgradingStrength && recentTicks.size < 3) return
            checkTargetSpeed()
        }

        fun getColorOffset(): Int {
            val target = targetColor ?: return -1
            val current = currentColor ?: return -1

            if (target < current) {
                return target + colorOrder.size - current
            }
            return target - current
        }

        fun getSpeedOffset(): Int {
            val target = targetSpeed ?: return -1
            val current = currentSpeed ?: return -1

            if (target < current) {
                return target + 5 - current
            }
            return target - current
        }

        fun getPitchOffset(): Int {
            val target = targetPitch ?: return -1
            val current = currentPitch ?: return -1

            if (target < current) {
                return target + 3 - current
            }
            return target - current
        }

        fun allCorrect(): Boolean {
            if (targetColor == null || targetSpeed == null || targetPitch == null) return false
            if (currentColor == null || currentSpeed == null || currentPitch == null) return false

            return getColorOffset() == 0 && getSpeedOffset() == 0 && getPitchOffset() == 0
        }

        fun checkTargetSpeed() {
            if (recentTicks.isEmpty()) return
            val recent = recentTicks.takeLast(10)
            val sorted = recent.sorted()
            val median = if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            } else {
                sorted[sorted.size / 2].toDouble()
            }

            val new = recent.filter { it.toDouble() in (median * 0.8)..(median * 1.2) }
            if (new.isEmpty()) return
            val speed = new.average()
            targetSpeed = speedMap.entries.minByOrNull { abs(it.key - speed.toInt()) }?.value ?: return
        }

        fun clear() {
            targetColor = null
            targetSpeed = null
            targetPitch = null
            isPaused = false
            currentColor = null
            currentSpeed = null
            currentPitch = null
            currentMatchSlot = MATCH_SLOTS.first
            recentTicks.clear()
            lastServerTickCount = 0
        }
    }
}
