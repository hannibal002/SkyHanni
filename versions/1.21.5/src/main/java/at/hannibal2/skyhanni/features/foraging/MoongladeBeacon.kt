package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.ResettableStorageSet
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.NotificationManager
import at.hannibal2.skyhanni.data.SkyHanniNotification
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.GuiContainerEvent
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
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.isEnchanted
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.ModernPatterns
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.filterNotEmptyString
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
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
    enum class BeaconColor(private val displayName: String, itemOverride: Item? = null) {
        WHITE("§fWhite"),
        ORANGE("§6Orange"),
        MAGENTA("§dMagenta"),
        LIGHT_BLUE("§9Light Blue"), // Why did hypixel do this
        YELLOW("§eYellow"),
        LIME("§aLime"),
        PINK("§dPink"),
        CYAN("§bCyan"), // This too
        PURPLE("§5Purple"),
        BLUE("§1Blue"), // This one makes sense ig
        BROWN("§6Brown"),
        GREEN("§2Green"),
        RED("§4Red"),
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
    enum class BeaconSpeed(val tickSpeed: Int, val guiSpeed: Int) {
        SPEED_5(52, 1),
        SPEED_4(42, 2),
        SPEED_3(32, 3),
        SPEED_2(22, 4),
        SPEED_1(12, 5),
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
    enum class BeaconPitch(private val displayName: String, val pitch: Float) {
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
    enum class BeaconSlotRange(
        private val displayName: String,
        val range: IntRange,
        val target: BeaconPieceTarget,
    ) {
        MATCH("Match Slots", 10..16, BeaconPieceTarget.REFERENCE),
        CHANGE("Change Slots", 28..34, BeaconPieceTarget.OURS),
        ;

        companion object {
            fun getByIndexOrNull(index: Int): BeaconSlotRange? = entries.find { index in it.range }
        }

        override fun toString(): String = displayName
    }

    private fun Enum<*>?.formatOrDefault(default: String = "§eUnknown"): String {
        return this?.toString() ?: default
    }
    // </editor-fold>

    private const val COLOR_SELECT_SLOT = 46
    private const val SPEED_SELECT_SLOT = 48
    private const val PITCH_SELECT_SLOT = 50
    private const val PAUSE_SELECT_SLOT = 52

    private val acceptablePitchMargin = 150.milliseconds

    private val colorMinigameInventory = InventoryDetector(
        openInventory = openInventory@{
            if (!solverEnabled()) return@openInventory
            currentServerTicks = 0
            checkPants()
            normalTuning = BeaconTuneData()
            enchantedTuning = BeaconTuneData(isEnchanted = true)
        },
        closeInventory = closeInventory@{
            if (!solverEnabled()) return@closeInventory
            normalTuning.reset()
            enchantedTuning.reset()
            display = emptyList()
        },
    ) { name ->
        upgradingStrength = (name == "Upgrade Signal Strength")
        val inInv = (name == "Tune Frequency" || upgradingStrength)
        inInv
    }

    private var upgradingStrength = false
    private var normalTuning = BeaconTuneData()
    private var enchantedTuning = BeaconTuneData(isEnchanted = true)
    private var display = emptyList<Renderable>()
    private var nextDevUpdate: SimpleTimeMark = SimpleTimeMark.farPast()

    private fun solverEnabled(): Boolean = colorMinigameInventory.isInside() && config.enabled

    private val STEREO_PANTS = "MUSIC_PANTS".toInternalName()

    private fun checkPants() {
        if (InventoryUtils.getLeggings()?.getInternalName() != STEREO_PANTS) return
        val text = "The solver may not work properly if you are wearing Stereo Pants!"
        NotificationManager.queueNotification(SkyHanniNotification(text, length = 5.seconds, showOverInventory = true))
    }

    private fun ItemStack.isPaused(): Boolean {
        return this.item == Items.RED_TERRACOTTA
    }

    @HandleEvent
    fun onTick() {
        if (!debugConfig.moongladeBeacon || nextDevUpdate.isInFuture()) return
        display = drawDisplay()
        nextDevUpdate = SimpleTimeMark.now() + 100.milliseconds
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!solverEnabled()) return
        if (event.blockOverClick()) {
            SoundUtils.playErrorSound()
            TitleManager.sendTitle(
                "§cOver-click Prevented",
                subtitleText = "§7Hold §eContol §7to bypass",
                duration = 1.seconds,
                location = TitleManager.TitleLocation.INVENTORY,
            )
            return event.cancel()
        }
        if (!config.useMiddleClick) return

        if (event.clickedButton != 0) return
        event.makePickblock()
    }

    private fun GuiContainerEvent.SlotClickEvent.blockOverClick(): Boolean {
        if (!config.preventOverClicking) return false
        if (KeyboardManager.isControlKeyDown()) return false
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

        val varianceSets = listOfNotNull(normalTuning, enchantedTuning.takeIf { upgradingStrength }).mapNotNull { set ->
            set.getLowestVariance()?.let { (target, duration) -> Triple(set, target, duration) }
        }

        val (bestSet, target, _) = varianceSets.filter { it.third <= acceptablePitchMargin }.minByOrNull {
            it.third.inWholeMilliseconds
        } ?: return

        bestSet.handlePitch(pitch, target)
    }

    init {
        RenderDisplayHelper(
            outsideInventory = false,
            inOwnInventory = false,
            inventory = colorMinigameInventory,
            condition = { config.enabled },
            onlyOnIsland = IslandType.GALATEA,
            onRender = {
                config.displayPosition.renderRenderables(display, posLabel = "Moonglade Beacon")
            },
        )
    }

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

    @HandleEvent(InventoryUpdatedEvent::class, onlyOnIsland = IslandType.GALATEA)
    fun onInventoryUpdated() {
        if (!solverEnabled()) return

        for (slot in InventoryUtils.getItemsInOpenChest().filter { it.hasStack() && it.stack.isNotEmpty() }) {
            val tuningData = if (slot.stack.isEnchanted()) enchantedTuning else normalTuning
            tuningData.readSlot(slot)
        }
        display = drawDisplay()
    }

    private fun Slot.performColorApplicableSet(block: (Pair<BeaconTuneData, BeaconColor>) -> Unit): Boolean {
        val tuningData = if (this.stack.isEnchanted()) enchantedTuning else normalTuning
        val stackColor = this.stack?.item?.getColorOrNull() ?: return false
        block.invoke(tuningData to stackColor)
        return true
    }

    private fun drawDisplay() = buildList {
        addAll(normalTuning.getRenderables())
        if (upgradingStrength) {
            addAll(enchantedTuning.getRenderables())
        }
    }

    enum class BeaconPieceTarget {
        REFERENCE,
        OURS,
    }

    open class DataPair<T : Any>(
        open var reference: T? = null,
        open var ours: T? = null,
    ) : ResettableStorageSet() {
        open operator fun set(target: BeaconPieceTarget, value: T?) = when (target) {
            BeaconPieceTarget.REFERENCE -> reference = value
            BeaconPieceTarget.OURS -> ours = value
        }

        operator fun get(target: BeaconPieceTarget): T? = when (target) {
            BeaconPieceTarget.REFERENCE -> reference
            BeaconPieceTarget.OURS -> ours
        }

        val asMap: Map<BeaconPieceTarget, T?>
            get() = mapOf(
                BeaconPieceTarget.REFERENCE to reference,
                BeaconPieceTarget.OURS to ours,
            )
    }

    class BeaconDataPair<T : Enum<T>>(
        override var reference: T? = null,
        override var ours: T? = null,
    ) : DataPair<T>(reference, ours)

    inline fun <reified E : Enum<E>> E.internalGetOffset(other: E): Int {
        val raw = this.ordinal - other.ordinal
        return if (raw < 0) raw + enumValues<E>().size else raw
    }

    inline fun <reified T : Enum<T>> BeaconDataPair<T>.getOffset(): Int? {
        val r = reference ?: return null
        val o = ours ?: return null
        return r.internalGetOffset(o)
    }

    data class BeaconTuneData(
        val isEnchanted: Boolean = false,
    ) : ResettableStorageSet() {
        private val debugName = if (isEnchanted) "§aEnchanted Tuning" else "§dNormal Tuning"
        private val title = if (isEnchanted) "§aEnchanted Tuning" else "§d§lMoonglade Beacon Solver"
        private val slotOffset = if (upgradingStrength && !isEnchanted) -9 else 0

        private val colorPair = BeaconDataPair<BeaconColor>()
        private val speedPair = BeaconDataPair<BeaconSpeed>()
        private val pitchPair = BeaconDataPair<BeaconPitch>()

        private val nextPitchPair = DataPair<SimpleTimeMark>()
        private val bufferPair = object : DataPair<MutableList<BeaconPitch>>(
            reference = mutableListOf(),
            ours = mutableListOf(),
        ) {
            override fun reset() {
                reference?.clear()
                ours?.clear()
            }
        }
        private val slotPair = object : DataPair<Int>() {
            override fun set(target: BeaconPieceTarget, value: Int?) {
                if (target == BeaconPieceTarget.REFERENCE) value?.let { updateMatchSlot(it) }
                super.set(target, value)
            }
        }

        private val colorSelectSlot = COLOR_SELECT_SLOT + slotOffset
        private val speedSelectSlot = SPEED_SELECT_SLOT + slotOffset
        private val pitchSelectSlot = PITCH_SELECT_SLOT + slotOffset
        private val pauseSelectSlot = PAUSE_SELECT_SLOT + slotOffset

        private var paused = false
        private var lastServerTickCount = 0
        private var recentTicks: MutableList<Int> = mutableListOf()
        private var currentRefSlot: Int = BeaconSlotRange.MATCH.range.first

        fun getLowestVariance(): Pair<BeaconPieceTarget, Duration>? = nextPitchPair.asMap.map { (target, mark) ->
            val duration = mark?.absoluteDifference(SimpleTimeMark.now()) ?: Duration.INFINITE
            target to duration
        }.minByOrNull { it.second.inWholeMilliseconds }

        fun handlePitch(pitch: BeaconPitch, target: BeaconPieceTarget) {
            recordPitch(pitch, forReference = target == BeaconPieceTarget.REFERENCE)
            pitchPair[BeaconPieceTarget.REFERENCE] = getAveragePitch(forReference = true) ?: pitchPair[BeaconPieceTarget.REFERENCE]
            pitchPair[BeaconPieceTarget.OURS] = getAveragePitch(forReference = false) ?: pitchPair[BeaconPieceTarget.OURS]
            nextPitchPair[target] = null
        }

        private fun recordPitch(pitch: BeaconPitch, forReference: Boolean) {
            val targetBuf = if (forReference) bufferPair.reference else {
                bufferPair.ours.takeIf { !paused } ?: return
            }
            val buf = targetBuf ?: return
            if (buf.size >= 20) buf.removeAt(0)
            buf.add(pitch)
            if (buf.distinct().size > 3) buf.clear()
        }

        private fun getAveragePitch(forReference: Boolean): BeaconPitch? {
            val targetBuf = if (forReference) bufferPair.reference else bufferPair.ours
            val buf = targetBuf?.takeIf { it.size >= 3 }
            return buf?.groupingBy { it }?.eachCount()?.maxByOrNull { it.value }?.key
        }

        fun allCorrect(): Boolean {
            val colorOffset = colorPair.getOffset<BeaconColor>()
            val speedOffset = speedPair.getOffset<BeaconSpeed>()
            val pitchOffset = pitchPair.getOffset<BeaconPitch>()
            return listOf(colorOffset, speedOffset, pitchOffset).all { it == 0 }
        }

        fun readSlot(slot: Slot) {
            if (readColorFromSlot(slot)) {
                readSlotFromSlot(slot)
            }

            readCurrentFromSlot(slot)
        }

        private fun readSlotFromSlot(slot: Slot): Boolean {
            val target = BeaconSlotRange.getByIndexOrNull(slot.index)?.target ?: return false
            if (slotPair[target] == slot.index) return false
            slotPair[target] = slot.index
            return true
        }

        private fun readColorFromSlot(slot: Slot): Boolean {
            val target = BeaconSlotRange.getByIndexOrNull(slot.index)?.target ?: return false
            return slot.performColorApplicableSet { (tuningData, color) ->
                tuningData.colorPair[target] = color
            }
        }

        private fun readCurrentFromSlot(slot: Slot) {
            val stack = slot.stack
            if (stack == null || (isEnchanted && !upgradingStrength)) return
            when (slot.index) {
                colorSelectSlot -> colorPair[BeaconPieceTarget.OURS] = slot.getLoreColorOrNull()
                speedSelectSlot -> {
                    speedPair[BeaconPieceTarget.OURS] = slot.getBeaconSpeedOrNull()
                    nextPitchPair[BeaconPieceTarget.OURS] = speedPair[BeaconPieceTarget.OURS]?.getOffsetFromNow()
                }

                pitchSelectSlot -> pitchPair[BeaconPieceTarget.OURS] = slot.getBeaconPitchOrNull()
                pauseSelectSlot -> paused = stack.isPaused()
            }
        }

        private fun updateMatchSlot(slot: Int) {
            currentRefSlot = slot.takeIf { it != currentRefSlot } ?: return
            val tickDifference = (currentServerTicks - lastServerTickCount).takeIf { it > 0 } ?: return
            recentTicks.add(tickDifference)
            lastServerTickCount = currentServerTicks
            if (upgradingStrength && recentTicks.size < 3) return
            checkReferenceSpeed()
        }

        fun tryHighlightSlot(slot: Slot): Boolean {
            if (isEnchanted && !upgradingStrength) return false

            if (slot.index == pitchSelectSlot) {
                val uiPitch = slot.getBeaconPitchOrNull() ?: return false
                if (uiPitch == pitchPair.reference) {
                    slot.highlight(LorenzColor.GREEN.addOpacity(200))
                    return true
                }
                return false
            }

            getOffsetBySlot(slot.index).takeIf { it == 0 } ?: return false
            slot.highlight(LorenzColor.GREEN.addOpacity(200))
            return true
        }

        @Suppress("HandleEventInspection")
        fun tryLabelIfAble(event: RenderInventoryItemTipEvent) {
            if (isEnchanted && !upgradingStrength) return
            val offset = getOffsetBySlot(event.slot.index)?.takeIf { it > 0 } ?: return
            event.stackTip = "§a$offset"
        }

        fun getOffsetBySlot(slot: Int): Int? = when (slot) {
            colorSelectSlot -> colorPair.getOffset()
            speedSelectSlot -> speedPair.getOffset()
            pitchSelectSlot -> pitchPair.getOffset()
            else -> null
        }

        private fun checkReferenceSpeed() {
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

            val referenceSpeed = BeaconSpeed.byClosestTickSpeed(calculatedSpeed) ?: return
            speedPair[BeaconPieceTarget.REFERENCE] = referenceSpeed
            nextPitchPair[BeaconPieceTarget.REFERENCE] = referenceSpeed.getOffsetFromNow()
        }

        override fun toString() = buildString {
            if (isEnchanted) appendLine(" ")
            appendLine(title)
            appendLine(" §7Ref Color: ${colorPair.reference.formatOrDefault()}")
            appendLine(" §7Ref Speed: §a${speedPair.reference.formatOrDefault("§eCalculating..")}")
            appendLine(" §7Ref Pitch: §a${pitchPair.reference.formatOrDefault()}")

            if (debugConfig.moongladeBeacon) {
                appendLine("  §8Our Color: ${colorPair.ours.formatOrDefault()}")
                appendLine("  §8Our Speed: §a${speedPair.ours.formatOrDefault()}")
                appendLine("  §8Our Pitch: §a${pitchPair.ours.formatOrDefault()}")
                appendLine("  §8Off Color: §a${colorPair.getOffset() ?: "§cUnknown"}")
                appendLine("  §8Off Speed: §a${speedPair.getOffset() ?: "§cUnknown"}")
                appendLine("  §8Off Pitch: §a${pitchPair.getOffset() ?: "§cUnknown"}")
                appendLine("  §8Next Ref Pitch: §a${nextPitchPair.reference?.timeUntil()?.format() ?: "§cUnknown"}")
                appendLine("  §8Next Our Pitch: §a${nextPitchPair.ours?.timeUntil()?.format() ?: "§cUnknown"}")
            }
        }

        fun getRenderables() = toString().split("\n").filterNotEmptyString().map(::StringRenderable)
    }
}
