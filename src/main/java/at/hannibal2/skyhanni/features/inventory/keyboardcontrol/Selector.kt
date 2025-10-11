package at.hannibal2.skyhanni.features.inventory.keyboardcontrol

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawBorder
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.Slot
import org.lwjgl.input.Keyboard

@SkyHanniModule
object Selector {

    private val config get() = SkyHanniMod.feature.inventory.keyboardControl

    private const val INITIAL_ROW = 3
    private const val INITIAL_COL = 4
    private const val COLUMN_COUNT = 9

    private data class SelectorPosition(val row: Int, val col: Int)

    private val positionCache = mutableMapOf<String, SelectorPosition>()
    private var currentPosition = SelectorPosition(INITIAL_ROW, INITIAL_COL)
    private var currentTitle = ""
    private var totalRows = 0

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryOpened(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return
        currentTitle = event.inventoryName.removeColor()

        updateTotalRows()

        var row = currentPosition.row
        var col = currentPosition.col
        if (config.inventorySelector.rememberPosition) {
            positionCache[currentTitle]?.let {
                row = it.row
                col = it.col
            }
        }
        row = row.coerceIn(0, totalRows - 1)
        col = col.coerceIn(0, COLUMN_COUNT - 1)
        currentPosition = SelectorPosition(row, col)
    }

    // event.inventoryItems is out of sync with currentScreen.inventorySlots, but we need to see all empty slots
    private fun updateTotalRows() {
        val gui = Minecraft.getMinecraft().currentScreen as? GuiChest ?: return
        totalRows = gui.inventorySlots.inventorySlots.size / COLUMN_COUNT
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiKeyPress(event: GuiKeyPressEvent) {
        if (!isEnabled()) return

        updateTotalRows()

        var row = currentPosition.row
        var col = currentPosition.col
        val oldRow = row
        val oldCol = col

        if (config.inventorySelector.up.isKeyHeld()) row--
        if (config.inventorySelector.down.isKeyHeld()) row++
        if (config.inventorySelector.left.isKeyHeld()) col--
        if (config.inventorySelector.right.isKeyHeld()) col++

        val moved = row != oldRow || col != oldCol
        var clicked = false
        if (moved) {
            if (config.inventorySelector.wrap) {
                row = (row + totalRows) % totalRows
                col = (col + COLUMN_COUNT) % COLUMN_COUNT
            } else {
                row = row.coerceIn(0, totalRows - 1)
                col = col.coerceIn(0, COLUMN_COUNT - 1)
            }
        }

        if (config.inventorySelector.click.isKeyHeld()) {
            getSelectedSlot(row, col)?.let { slot ->
                val mouseButton = if (Keyboard.KEY_LCONTROL.isKeyHeld()) 1 else 0
                val mode = if (Keyboard.KEY_LSHIFT.isKeyHeld()) 1 else 0
                InventoryUtils.clickSlot(slot.slotNumber, mouseButton = mouseButton, mode = mode)
                clicked = true
            }
        }

        if (moved || clicked) {
            event.cancel()
            currentPosition = SelectorPosition(row, col)
            if (config.inventorySelector.rememberPosition) {
                positionCache[currentTitle] = currentPosition
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        getSelectedSlot(currentPosition.row, currentPosition.col)?.drawBorder(LorenzColor.BLUE, 2)
    }

    private fun getSelectedSlot(row: Int, col: Int): Slot? {
        val gui = Minecraft.getMinecraft().currentScreen as? GuiChest ?: return null
        val slotNumber = row * COLUMN_COUNT + col
        val slots = gui.inventorySlots.inventorySlots
        if (slotNumber < 0 || slotNumber >= slots.size) return null
        return slots.find { it.slotNumber == slotNumber }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.selectorEnabled
}
