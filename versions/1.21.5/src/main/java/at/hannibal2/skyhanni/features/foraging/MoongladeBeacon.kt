package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.minecraft.ServerTickEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.isEnchanted
import at.hannibal2.skyhanni.utils.ModernPatterns
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

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
        "White",
        "Orange",
        "Magenta",
        "Light Blue",
        "Yellow",
        "Lime",
        "Pink",
        "Cyan",
        "Purple",
        "Blue",
        "Brown",
        "Green",
        "Red",
    )

    private val pitchLevels = listOf(
        "Low",
        "Normal",
        "High",
    )

    private const val COLOR_SELECT_SLOT = 46
    private const val SPEED_SELECT_SLOT = 48
    private const val PITCH_SELECT_SLOT = 50
    private const val PAUSE_SELECT_SLOT = 52

    private val MATCH_SLOTS = 10..16
    private val CHANGE_SLOTS = 28..34

    private var inInventory = false
    private var enchantedEnabled = false

    private var normalTuning = TuneData()
    private var enchantedTuning = TuneData(isEnchanted = true)

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        inInventory = event.inventoryName == "Tune Frequency"
        if (!inInventory) return
        serverTicksSinceUpdate = 0
        testSlot = 0
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

    private var ticksSinceUpdate = 0
    private var serverTicksSinceUpdate = 0

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!inInventory) return
        ticksSinceUpdate++
    }

    @HandleEvent
    fun onServerTick(event: ServerTickEvent) {
        if (!inInventory) return
        serverTicksSinceUpdate++
    }

    private var testSlot = 0

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!inInventory) return
        val slots = InventoryUtils.getItemsInOpenChest()

        for (slot in slots) {
            when (slot.index) {
                in MATCH_SLOTS -> {
                    val colorIndex = slot.stack.getColorIndex()
                    if (colorIndex == -1) continue
                    val isItemEnchanted = slot.stack.isEnchanted()
                    val tuningData = if (isItemEnchanted) enchantedTuning else normalTuning
                    tuningData.targetColor = colorIndex
                    tuningData.currentMatchSlot = slot.index
                }

                in CHANGE_SLOTS -> {
                    val colorIndex = slot.stack.getColorIndex()
                    if (colorIndex == -1) continue
                    val isItemEnchanted = slot.stack.isEnchanted()
                    val tuningData = if (isItemEnchanted) enchantedTuning else normalTuning
                    tuningData.currentColor = colorIndex
                    if (slot.index == testSlot) continue
                    ChatUtils.chat("ticks since update: $serverTicksSinceUpdate, ticks since update: $ticksSinceUpdate")
                    serverTicksSinceUpdate = 0
                    ticksSinceUpdate = 0
                    testSlot = slot.index
                }

                normalTuning.colorSelectSlot -> {
                    val color = slot.stack.getColorFromItem() ?: continue
                    normalTuning.targetColor = color
                }

                enchantedTuning.colorSelectSlot -> {
                    if (!enchantedEnabled) continue
                    val color = slot.stack.getColorFromItem() ?: continue
                    enchantedTuning.targetColor = color
                }

                normalTuning.speedSelectSlot -> {
                    val speed = slot.stack.getSpeedFromItem() ?: continue
                    normalTuning.targetSpeed = speed
                }

                enchantedTuning.speedSelectSlot -> {
                    if (!enchantedEnabled) continue
                    val speed = slot.stack.getSpeedFromItem() ?: continue
                    enchantedTuning.targetSpeed = speed
                }

                normalTuning.pitchSelectSlot -> {
                    val pitch = slot.stack.getPitchFromItem() ?: continue
                    normalTuning.targetPitch = pitch
                }

                enchantedTuning.pitchSelectSlot -> {
                    if (!enchantedEnabled) continue
                    val pitch = slot.stack.getPitchFromItem() ?: continue
                    enchantedTuning.targetPitch = pitch
                }

                normalTuning.pauseSelectSlot -> {
                    normalTuning.isPaused = slot.stack.isPaused()
                }

                enchantedTuning.pauseSelectSlot -> {
                    if (!enchantedEnabled) continue
                    enchantedTuning.isPaused = slot.stack.isPaused()
                }
            }
        }
    }

    private fun ItemStack.getColorIndex(): Int {
        return colorOrder.indexOf(this.item)
    }

    private fun ItemStack.getColorFromItem(): Int? {
        val lore = getLore().map { it.removeColor() }
        ModernPatterns.currentColorPattern.firstMatcher(lore) {
            val colorName = group("color")
            return colorOrderNames.indexOf(colorName).takeIf { it >= 0 }
        }
        return null
    }

    private fun ItemStack.getSpeedFromItem(): Int? {
        val lore = getLore().map { it.removeColor() }
        ModernPatterns.currentSpeedPattern.firstMatcher(lore) {
            val speed = group("speed")?.formatIntOrNull() ?: return@firstMatcher null
            return speed
        }
        return null
    }

    private fun ItemStack.getPitchFromItem(): Int? {
        val lore = getLore().map { it.removeColor() }
        ModernPatterns.currentPitchPattern.firstMatcher(lore) {
            val pitchName = group("pitch") ?: return@firstMatcher null
            return pitchLevels.indexOf(pitchName).takeIf { it >= 0 }
        }
        return null
    }

    private fun ItemStack.isPaused(): Boolean {
        return this.item == Items.RED_TERRACOTTA
    }

    private data class TuneData(
        var targetColor: Int? = null,
        var targetSpeed: Int? = null,
        var targetPitch: Int? = null,
        var isPaused: Boolean = false,
        var currentColor: Int? = null,
        var currentSpeed: Int? = null,
        var currentPitch: Int? = null,
        val isEnchanted: Boolean = false,
        var currentMatchSlot: Int = MATCH_SLOTS.first
    ) {
        val slotOffset = if (isEnchanted) -9 else 0
        val colorSelectSlot = COLOR_SELECT_SLOT + slotOffset
        val speedSelectSlot = SPEED_SELECT_SLOT + slotOffset
        val pitchSelectSlot = PITCH_SELECT_SLOT + slotOffset
        val pauseSelectSlot = PAUSE_SELECT_SLOT + slotOffset

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

        fun clear() {
            targetColor = null
            targetSpeed = null
            targetPitch = null
            isPaused = false
            currentColor = null
            currentSpeed = null
            currentPitch = null
            currentMatchSlot = MATCH_SLOTS.first
        }
    }
}
