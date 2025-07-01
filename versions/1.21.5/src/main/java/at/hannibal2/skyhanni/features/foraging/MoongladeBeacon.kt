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
import at.hannibal2.skyhanni.features.foraging.MoongladeBeacon.BeaconColor.Companion.getColorOrNull
import at.hannibal2.skyhanni.features.foraging.MoongladeBeacon.BeaconColor.Companion.getLoreColorOrNull
import at.hannibal2.skyhanni.features.foraging.MoongladeBeacon.BeaconPitch.Companion.getBeaconPitchOrNull
import at.hannibal2.skyhanni.features.foraging.MoongladeBeacon.BeaconSpeed.Companion.getBeaconSpeedOrNull
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
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
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.times

@SkyHanniModule
object MoongladeBeacon {

    private val config get() = SkyHanniMod.feature.foraging.moongladeBeacon
    private val debugConfig get() = SkyHanniMod.feature.dev.debug

    // <editor-fold desc="Enums & Enum Helpers">
    /**
     * Represents the order of colors for the beacon minigame.
     * Attempts to auto-fetch the item from the registry if not provided.
     *
     * @param displayName The display name of the color as shown in the GUI.
     * @param itemOverride Optional override for the item to use for this color.
     */
    private enum class BeaconColor(private val displayName: String, itemOverride: Item? = null) {
        WHITE("§fWhite"),
        ORANGE("§6Orange"),
        MAGENTA("§dMagenta"),
        LIGHT_BLUE("§bLight Blue"),
        YELLOW("§eYellow"),
        LIME("§aLime"),
        PINK("§dPink"),
        CYAN("§bCyan"),
        PURPLE("§5Purple"),
        BLUE("§1Blue"),
        BROWN("§6Brown"),
        GREEN("§2Green"),
        RED("§cRed"),
        ;

        override fun toString() = displayName

        private val identifier = Identifier.of("minecraft", name.lowercase() + "_stained_glass_pane")
        val item by lazy { itemOverride ?: Registries.ITEM.get(identifier) }

        companion object {
            fun Item.getColorOrNull(): BeaconColor? = entries.find { it.item == this@getColorOrNull }
            fun Slot.getLoreColorOrNull(): BeaconColor? {
                val stack = this.stack ?: return null
                return ModernPatterns.beaconCurrentColorPattern.firstMatcher(stack.getLore()) {
                    val colorName = group("color") ?: return@firstMatcher null
                    entries.find { it.displayName.equals(colorName, ignoreCase = true) }
                }
            }
        }
    }

    /**
     * Represents the Speed levels for the beacon minigame.
     *
     * @param tickSpeed The number of ticks it takes to move (one slot) at this speed level.
     * @param guiSpeed The speed level as displayed in the GUI (1-5).
     */
    private enum class BeaconSpeed(val tickSpeed: Int, val guiSpeed: Int) {
        SPEED_1(12, 5),
        SPEED_2(22, 4),
        SPEED_3(32, 3),
        SPEED_4(42, 2),
        SPEED_5(52, 1),
        ;

        override fun toString() = "§aSpeed $guiSpeed"

        fun getOffsetFromNow(): SimpleTimeMark =
            SimpleTimeMark.now() + (tickSpeed * 50.milliseconds)

        companion object {
            fun byClosestTickSpeed(measuredTickSpeed: Number) = entries.minByOrNull { speed ->
                abs(speed.tickSpeed - measuredTickSpeed.toInt())
            }
            fun Slot.getBeaconSpeedOrNull(): BeaconSpeed? {
                val stack = this.stack ?: return null
                return ModernPatterns.beaconCurrentSpeedPattern.firstMatcher(stack.getLore()) {
                    val guiSpeed = group("speed")?.formatIntOrNull() ?: return@firstMatcher null
                    entries.find { it.guiSpeed == guiSpeed }
                }
            }
        }
    }

    /**
     * Represents the pitch levels for the beacon minigame.
     *
     * @param displayName The display name of the pitch as shown in the GUI.
     * @param pitch The pitch value used in the sound system.
     */
    private enum class BeaconPitch(private val displayName: String, val pitch: Float) {
        LOW("Low", 0.0952381f),
        NORMAL("Normal", 0.7936508f),
        HIGH("High", 1.4920635f),
        ;

        override fun toString(): String = displayName

        companion object {
            fun getByPitch(pitch: Float): BeaconPitch? = entries.find { it.pitch == pitch }
            fun Slot.getBeaconPitchOrNull(): BeaconPitch? {
                val stack = this.stack ?: return null
                return ModernPatterns.beaconCurrentPitchPattern.firstMatcher(stack.getLore()) {
                    entries.find { it.displayName.equals(group("pitch"), ignoreCase = true) }
                }
            }
        }
    }

    /**
     * Represents a range of slots we're interested in reading.
     *
     * @param displayName The display name of the slot range for debugging purposes.
     * @param range The range of slots (inclusive) that this enum covers.
     */
    private enum class SlotRange(
        private val displayName: String,
        val range: IntRange,
    ) {
        MATCH("Match Slots", 10..16) {
            override fun Pair<BeaconColor, TuneData>.slotMod(slot: Slot) {
                second.targetColor = first
                second.updateMatchSlot(slot.index)
            }
        },
        CHANGE("Change Slots", 28..34){
            override fun Pair<BeaconColor, TuneData>.slotMod(slot: Slot) {
                second.currentColor = first
            }
        },
        ;

        fun handleSlot(slot: Slot): Boolean {
            return if (slot.index !in range) false
            else slot.performColorApplicableSet { it.slotMod(slot) }
        }

        protected abstract fun Pair<BeaconColor, TuneData>.slotMod(slot: Slot)
        override fun toString(): String = displayName
    }

    private fun Enum<*>?.formatOrDefault(default: String = "§eUnknown"): String {
        return this?.toString() ?: default
    }

    private inline fun <reified E : Enum<E>> E.getOffset(other: E): Int {
        val raw = this.ordinal - other.ordinal
        return if (raw < 0) raw + enumValues<E>().size else raw
    }
    // </editor-fold>

    private const val COLOR_SELECT_SLOT = 46
    private const val SPEED_SELECT_SLOT = 48
    private const val PITCH_SELECT_SLOT = 50
    private const val PAUSE_SELECT_SLOT = 52

    private val acceptableMargin = 50.milliseconds // ~2.5 ticks

    private val colorMinigameInventory = InventoryDetector(
        onInventoryClose = {
            normalTuning.clear()
            enchantedTuning.clear()
            display = emptyList()
        },
        openInventory = {
            normalTuning = TuneData()
            enchantedTuning = TuneData(isEnchanted = true)
        }
    ) { name ->
        upgradingStrength = (name == "Upgrade Signal Strength")
        val inInv = (name == "Tune Frequency" || upgradingStrength)
        inInv
    }

    private var upgradingStrength = false
    private var normalTuning = TuneData()
    private var enchantedTuning = TuneData(isEnchanted = true)
    private var display = emptyList<Renderable>()

    private fun solverEnabled(): Boolean = colorMinigameInventory.isInside() && config.enabled

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!solverEnabled()) return
        currentServerTicks = 0
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!solverEnabled()) return
        if (event.blockOverClick()) return event.cancel()
        if (!config.useMiddleClick) return

        if (event.clickedButton != 0) return
        event.makePickblock()
    }

    private fun GuiContainerEvent.SlotClickEvent.blockOverClick(): Boolean {
        if (!config.preventOverClicking) return false
        val slotIndex = this.slot?.index ?: return false
        val neededClickOffset = normalTuning.getOffsetBySlot(slotIndex)
            ?: enchantedTuning.getOffsetBySlot(slotIndex)?.takeUnless { !upgradingStrength }
            ?: return false
        return neededClickOffset == 0
    }

    private var currentServerTicks = 0

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onServerTick(event: ServerTickEvent) {
        if (!colorMinigameInventory.isInside()) return
        currentServerTicks++
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onPlaySound(event: PlaySoundEvent) {
        if (!colorMinigameInventory.isInside() || event.soundName != "note.bassattack") return
        val pitch = BeaconPitch.getByPitch(event.pitch) ?: return
        val targetTuneData = listOf(
            normalTuning, enchantedTuning
        ).minByOrNull {
            it.reportPitchTimeVariance(pitch) ?: Duration.INFINITE
        } ?: return

        if (targetTuneData.currentPitch == pitch) return
        targetTuneData.targetPitch = pitch
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!solverEnabled()) return
        config.displayPosition.renderRenderables(display, posLabel = "Moonglade Beacon")
    }

    private val highlightGreen = LorenzColor.GREEN.addOpacity(200)

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!solverEnabled()) return
        InventoryUtils.getItemsInOpenChest().forEach { slot ->
            if (normalTuning.tryHighlightSlot(slot)) return@forEach
            if (enchantedTuning.tryHighlightSlot(slot)) return@forEach
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!solverEnabled()) return
        normalTuning.tryLabelIfAble(event)
        enchantedTuning.tryLabelIfAble(event)
    }

    private fun RenderInventoryItemTipEvent.labelIfAble(label: Int) {
        if (label <= 0) return
        stackTip = "§a$label"
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!solverEnabled()) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (normalTuning.readFromSlotLore(slot)) continue
            else if (enchantedTuning.readFromSlotLore(slot)) continue
            else if (SlotRange.MATCH.handleSlot(slot)) continue
            else if (SlotRange.CHANGE.handleSlot(slot)) continue
        }
        display = drawDisplay()
    }

    private fun Slot.performColorApplicableSet(block: (Pair<BeaconColor, TuneData>) -> Unit): Boolean {
        val stackColor = this.stack?.item?.getColorOrNull() ?: return false
        val tuningData = if (this.stack.isEnchanted()) enchantedTuning else normalTuning
        block.invoke(stackColor to tuningData)
        return true
    }

    private fun drawDisplay() = buildList {
        addAll(normalTuning.getRenderables())
        if (upgradingStrength) {
            addAll(enchantedTuning.getRenderables())
        }
    }

    private data class TuneData(
        val isEnchanted: Boolean = false,
    ) {
        private val debugName = if (isEnchanted) "§aEnchanted Tuning" else "§dNormal Tuning"
        private val title = if (isEnchanted) "§aEnchanted Tuning" else "§d§lMoonglade Beacon Solver"
        private val slotOffset = if (upgradingStrength && !isEnchanted) -9 else 0

        var targetLastMoved: SimpleTimeMark? = null
        var targetColor: BeaconColor? = null
        var targetSpeed: BeaconSpeed? = null
        var targetPitch: BeaconPitch? = null
        var currentColor: BeaconColor? = null
        var currentSpeed: BeaconSpeed? = null
        var currentPitch: BeaconPitch? = null
        var nextExpectedPitch: SimpleTimeMark? = null

        private var lastServerTickCount = 0
        private var recentTicks: MutableList<Int> = mutableListOf()
        private var currentMatchSlot: Int = SlotRange.MATCH.range.first

        val colorSelectSlot = COLOR_SELECT_SLOT + slotOffset
        val speedSelectSlot = SPEED_SELECT_SLOT + slotOffset
        val pitchSelectSlot = PITCH_SELECT_SLOT + slotOffset
        val pauseSelectSlot = PAUSE_SELECT_SLOT + slotOffset

        val colorOffset: Int? get() = currentColor?.let { targetColor?.getOffset(it) }
        val speedOffset: Int? get() = currentSpeed?.let { targetSpeed?.getOffset(it) }
        val pitchOffset: Int? get() = currentPitch?.let { targetPitch?.getOffset(it) }

        fun allCorrect(): Boolean = colorOffset == 0 && speedOffset == 0 && pitchOffset == 0

        fun reportPitchTimeVariance(pitch: BeaconPitch): Duration? {
            if (isEnchanted && !upgradingStrength) return null
            val lastTargetMove = targetLastMoved ?: return null
            if (lastTargetMove.passedSince() > acceptableMargin) return null
            val timeUntil = nextExpectedPitch?.timeUntil() ?: 10.minutes
            val timeSince = nextExpectedPitch?.passedSince() ?: 10.minutes
            return minOf(timeSince, timeUntil).takeIf { it < acceptableMargin }
        }

        fun readFromSlotLore(slot: Slot): Boolean {
            val stack = slot.stack
            if (stack == null || (isEnchanted && !upgradingStrength)) return false
            when (slot.index) {
                colorSelectSlot -> currentColor = slot.getLoreColorOrNull()
                speedSelectSlot -> currentSpeed = slot.getBeaconSpeedOrNull()
                pitchSelectSlot -> currentPitch = slot.getBeaconPitchOrNull()
                else -> return false
            }
            return true
        }

        fun updateMatchSlot(slot: Int) {
            currentMatchSlot = slot.takeIf { it != currentMatchSlot } ?: return
            targetLastMoved = SimpleTimeMark.now()
            val tickDifference = (currentServerTicks - lastServerTickCount).takeIf { it > 0 } ?: return
            recentTicks.add(tickDifference)
            lastServerTickCount = currentServerTicks
            if (upgradingStrength && recentTicks.size < 3) return
            checkTargetSpeed()
        }

        fun tryHighlightSlot(slot: Slot): Boolean {
            if (isEnchanted && !upgradingStrength) return false
            val offset = getOffsetBySlot(slot.index) ?: return false
            if (offset == 0) return false
            slot.highlight(highlightGreen)
            return true
        }

        fun tryLabelIfAble(event: RenderInventoryItemTipEvent) {
            if (isEnchanted && !upgradingStrength) return
            val offset = getOffsetBySlot(event.slot.index) ?: return
            if (offset == 0) return
            event.labelIfAble(offset)
        }

        fun getOffsetBySlot(slot: Int): Int? = when (slot) {
            colorSelectSlot -> colorOffset
            speedSelectSlot -> speedOffset
            pitchSelectSlot -> pitchOffset
            else -> null
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

            val newTicks = recent.filter {
                it.toDouble() in (median * 0.8)..(median * 1.2)
            }.takeIfNotEmpty() ?: return
            val calculatedSpeed = newTicks.average()

            targetSpeed = BeaconSpeed.byClosestTickSpeed(calculatedSpeed) ?: return
            val targetSpeed = this.targetSpeed ?: return
            nextExpectedPitch = targetSpeed.getOffsetFromNow()
        }

        // todo make this class a ResettableStorageSet so we don't need this
        fun clear() {
            targetColor = null
            targetSpeed = null
            targetPitch = null
            currentColor = null
            currentSpeed = null
            currentPitch = null
            nextExpectedPitch = null
            currentMatchSlot = SlotRange.MATCH.range.first
            recentTicks.clear()
            lastServerTickCount = 0
        }

        override fun toString() = buildString {
            if (isEnchanted) appendLine()
            appendLine(title)
            appendLine(" §7Target Color: ${targetColor.formatOrDefault()}")
            appendLine(" §7Target Speed: §a${targetSpeed.formatOrDefault("§eCalculating..")}")
            appendLine(" §7Target Pitch: §a${targetPitch.formatOrDefault()}")
            if (debugConfig.moongladeBeacon) {
                appendLine("  §8Current Color: ${currentColor.formatOrDefault()}")
                appendLine("  §8Current Speed: §a${currentSpeed.formatOrDefault()}")
                appendLine("  §8Current Pitch: §a${currentPitch.formatOrDefault()}")
                appendLine("  §8Color Offset: §a${colorOffset ?: "§cUnknown"}")
                appendLine("  §8Speed Offset: §a${speedOffset ?: "§cUnknown"}")
                appendLine("  §8Pitch Offset: §a${pitchOffset ?: "§cUnknown"}")
                appendLine("  §8Next Expected Pitch: §a${nextExpectedPitch?.timeUntil()?.format() ?: "§cUnknown"}")
            }
        }

        fun getRenderables() = toString().split("\n").filter {
          it.isNotEmpty()
        }.map(::StringRenderable)
    }
}
