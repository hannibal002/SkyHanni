package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.events.minecraft.ServerTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.isEnchanted
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.ModernPatterns
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.slot.Slot
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

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

    private val colorMinigameInventory = InventoryDetector(
        onInventoryClose = {
            normalTuning.clear()
            enchantedTuning.clear()
            display = emptyList()
        }
    ) { name ->
        upgradingStrength = (name == "Upgrade Signal Strength")
        val inInv = (name == "Tune Frequency" || upgradingStrength)
        if (inInv) {
            normalTuning = TuneData()
            enchantedTuning = TuneData(isEnchanted = true)
            ChatUtils.debug("Normal tuning readable slots: ${normalTuning.readableSlots}")
            ChatUtils.debug("Enchanted tuning readable slots: ${enchantedTuning.readableSlots}")
        }
        inInv
    }

    private var upgradingStrength = false

    private enum class TuneSetType { NORMAL, ENCHANTED }

    private var nextExpectedPitch: MutableMap<TuneSetType, SimpleTimeMark> = enumMapOf()
    private var normalTuning = TuneData()
    private var enchantedTuning = TuneData(isEnchanted = true)
    private var display = emptyList<Renderable>()

    private fun solverEnabled(): Boolean = colorMinigameInventory.isInside() && config.enabled

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!colorMinigameInventory.isInside()) return
        currentServerTicks = 0
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!colorMinigameInventory.isInside()) return
        if (event.blockOverClick()) return event.cancel()
        if (!config.useMiddleClick) return

        if (event.clickedButton != 0) return
        event.makePickblock()
    }

    private fun GuiContainerEvent.SlotClickEvent.blockOverClick(): Boolean {
        if (!config.preventOverClicking) return false
        val slot = this.slot ?: return false
        val neededClickOffset = when (slot.index) {
            normalTuning.colorSelectSlot -> normalTuning.getColorOffset()
            normalTuning.speedSelectSlot -> normalTuning.getSpeedOffset()
            normalTuning.pitchSelectSlot -> normalTuning.getPitchOffset()
            enchantedTuning.colorSelectSlot -> if (upgradingStrength) enchantedTuning.getColorOffset() else null
            enchantedTuning.speedSelectSlot -> if (upgradingStrength) enchantedTuning.getSpeedOffset() else null
            enchantedTuning.pitchSelectSlot -> if (upgradingStrength) enchantedTuning.getPitchOffset() else null
            else -> null
        }

        return if (neededClickOffset == null) false
        else neededClickOffset == 0
    }

    private var currentServerTicks = 0

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onServerTick(event: ServerTickEvent) {
        if (!colorMinigameInventory.isInside()) return
        currentServerTicks++
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onPlaySound(event: PlaySoundEvent) {
        if (!colorMinigameInventory.isInside()) return
        if (event.soundName != "note.bassattack") return
        val pitch = pitchMap[event.pitch] ?: return
        if (upgradingStrength) {
            val targetSet = listOf(
                normalTuning,
                enchantedTuning,
            ).mapNotNull { tuneData ->
                // todo make this account for ping, possibly?
                val acceptableMargin = 100.milliseconds
                val timeUntil = tuneData.nextExpectedPitch?.timeUntil() ?: 1.minutes
                val timeSince = tuneData.nextExpectedPitch?.passedSince() ?: 1.minutes
                val isAcceptable = timeUntil < acceptableMargin || timeSince < acceptableMargin
                if (isAcceptable) tuneData else null
            }.minByOrNull {
                it.nextExpectedPitch ?: SimpleTimeMark.farPast()
            } ?: return

            if (targetSet.targetPitch == null) return
            if (targetSet.currentPitch == pitch) return
            targetSet.targetPitch = pitch
        } else {
            if (normalTuning.targetPitch == null) return
            if (normalTuning.currentPitch == pitch) return
            normalTuning.targetPitch = pitch
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!solverEnabled()) return

        config.displayPosition.renderRenderables(display, posLabel = "Moonglade Beacon")
    }

    private val highlightGreen = LorenzColor.GREEN.addOpacity(200)

    private fun Slot.highlightFromTuningSet(tuningSet: TuneData) {
        if (tuningSet.isEnchanted && !upgradingStrength) return
        when (index) {
            tuningSet.colorSelectSlot -> if (tuningSet.getColorOffset() != 0) return
            tuningSet.speedSelectSlot -> if (tuningSet.getSpeedOffset() != 0) return
            tuningSet.pitchSelectSlot -> if (tuningSet.getPitchOffset() != 0) return
        }
        highlight(highlightGreen)
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!solverEnabled()) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            when (slot.index) {
                in normalTuning.readableSlots -> slot.highlightFromTuningSet(normalTuning)
                in enchantedTuning.readableSlots -> slot.highlightFromTuningSet(enchantedTuning)
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
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

    private var lastUpdated: MutableMap<TuneSetType, SimpleTimeMark> = enumMapOf()

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!colorMinigameInventory.isInside()) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            when (slot.index) {
                in MATCH_SLOTS -> slot.handleMatchSlot()
                in CHANGE_SLOTS -> slot.handleChangeSlot()
                in normalTuning.readableSlots -> slot.readToTuningSet(normalTuning)
                in enchantedTuning.readableSlots -> slot.readToTuningSet(enchantedTuning)
            }
        }
        updateDisplay()
    }

    private fun Slot.handleMatchSlot() {
        val colorIndex = stack.getColorIndexOrNull() ?: return
        val isItemEnchanted = stack.isEnchanted()
        val tuningData = if (isItemEnchanted) enchantedTuning else normalTuning
        tuningData.targetColor = colorIndex
        tuningData.updateMatchSlot(index)
    }

    private fun Slot.handleChangeSlot() {
        val colorIndex = stack.getColorIndexOrNull() ?: return
        val isItemEnchanted = stack.isEnchanted()
        val tuningData = if (isItemEnchanted) enchantedTuning else normalTuning
        tuningData.currentColor = colorIndex
    }

    private fun Slot.readToTuningSet(tuningSet: TuneData) {
        if (tuningSet.isEnchanted && !upgradingStrength) return
        when (index) {
            tuningSet.colorSelectSlot -> {
                tuningSet.currentColor = stack.getColorFromItem() ?: return
            }
            tuningSet.speedSelectSlot -> {
                tuningSet.currentSpeed = stack.getSpeedFromItem() ?: return
            }
            tuningSet.pitchSelectSlot -> {
                val pitch = stack.getPitchFromItem() ?: return
                tuningSet.currentPitch = pitch
                if (tuningSet.targetPitch == null) tuningSet.targetPitch = pitch
            }
        }
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
        if (pitch == null) return "§eUnknown"
        return pitchLevels.getOrNull(pitch) ?: "§eUnknown"
    }

    private fun ItemStack.getColorIndexOrNull(): Int? = colorOrder.indexOf(this.item).takeIf {
        it != -1
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
        var nextExpectedPitch: SimpleTimeMark? = null

        private var lastServerTickCount = 0
        private var currentMatchSlot: Int = MATCH_SLOTS.first

        private var recentTicks: MutableList<Int> = mutableListOf()

        private val slotOffset = if (upgradingStrength && !isEnchanted) -9 else 0
        val colorSelectSlot = COLOR_SELECT_SLOT + slotOffset
        val speedSelectSlot = SPEED_SELECT_SLOT + slotOffset
        val pitchSelectSlot = PITCH_SELECT_SLOT + slotOffset
        val pauseSelectSlot = PAUSE_SELECT_SLOT + slotOffset

        val readableSlots get() = listOf(
            colorSelectSlot,
            speedSelectSlot,
            pitchSelectSlot,
        )

        fun updateMatchSlot(slot: Int) {
            currentMatchSlot = slot.takeIf { it != currentMatchSlot } ?: return
            val tickDifference = (currentServerTicks - lastServerTickCount).takeIf { it > 0 } ?: return
            recentTicks.add(tickDifference)
            lastServerTickCount = currentServerTicks
            if (upgradingStrength && recentTicks.size < 3) return
            checkTargetSpeed()
        }

        var lastOffsetColorMessage: String? = null
        var lastOffsetSpeedMessage: String? = null
        var lastOffsetPitchMessage: String? = null

        fun getColorOffset(): Int {
            val target = targetColor ?: return -1
            val current = currentColor ?: return -1
            val diff = if (target < current) target + colorOrder.size - current
            else target - current

            "Target Color: $target, Current Color: $current, Diff Color: $diff".takeIf {
                it != lastOffsetColorMessage
            }?.let {
                lastOffsetColorMessage = it
                ChatUtils.debug("${if (isEnchanted) "§aEnchanted " else ""}Tuning§r: $it")
            }

            return diff
        }

        fun getSpeedOffset(): Int {
            val target = targetSpeed ?: return -1
            val current = currentSpeed ?: return -1

            "Target Speed: $target, Current Speed: $current, Diff Speed: ${target - current}".takeIf {
                it != lastOffsetSpeedMessage
            }?.let {
                lastOffsetSpeedMessage = it
                ChatUtils.debug("${if (isEnchanted) "§aEnchanted " else ""}Tuning§r: $it")
            }

            if (target < current) {
                return target + 5 - current
            }
            return target - current
        }

        fun getPitchOffset(): Int {
            val target = targetPitch ?: return -1
            val current = currentPitch ?: return -1

            "Target Pitch: $target, Current Pitch: $current, Diff Pitch: ${target - current}".takeIf {
                it != lastOffsetPitchMessage
            }?.let {
                lastOffsetPitchMessage = it
                ChatUtils.debug("${if (isEnchanted) "§aEnchanted " else ""}Tuning§r: $it")
            }

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
            val targetSpeed = this.targetSpeed ?: return
            // .5s delay per speed level
            // todo account for ping
            nextExpectedPitch = SimpleTimeMark.now() + (targetSpeed * 500).milliseconds
        }

        fun clear() {
            targetColor = null
            targetSpeed = null
            targetPitch = null
            isPaused = false
            currentColor = null
            currentSpeed = null
            currentPitch = null
            nextExpectedPitch = null
            currentMatchSlot = MATCH_SLOTS.first
            recentTicks.clear()
            lastServerTickCount = 0
        }
    }
}
