package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import java.util.regex.Pattern

/**
 * The InventoryDetector tracks whether an inventory is open and provides
 * an inventory open consumer and a isInside function to handle inventory check logic.
 *
 * @property openInventory A callback triggered when the given inventory is detected to be open. Optional.
 * @property closeInventory A callback triggered when the inventory is closed. Optional.
 * @property checkInventoryName Define what inventory name or names we are looking for.
 */
class InventoryDetector(
    val openInventory: (InventoryFullyOpenedEvent) -> Unit = {},
    val closeInventory: (InventoryCloseEvent) -> Unit = {},
    val checkInventoryName: (String) -> Boolean,
) {
    constructor(
        pattern: Pattern,
        openInventory: (InventoryFullyOpenedEvent) -> Unit = {},
        closeInventory: (InventoryCloseEvent) -> Unit = {},
    ) : this(
        openInventory,
        checkInventoryName = { name -> pattern.matches(name) }
    )

    init {
        detectors.add(this)
    }

    private var inInventory = false

    /**
     * Check if the player is currently inside this inventory.
     */
    fun isInside() = inInventory

    @SkyHanniModule
    companion object {
        private val detectors = mutableListOf<InventoryDetector>()

        @HandleEvent(priority = HandleEvent.HIGHEST)
        fun onInventoryClose(event: InventoryCloseEvent) {
            detectors.forEach {
                it.inInventory = false
                it.closeInventory(event)
            }
        }

        @HandleEvent(priority = HandleEvent.HIGHEST)
        fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
            detectors.forEach { it.updateInventoryState(event) }
        }
    }

    private fun updateInventoryState(event: InventoryFullyOpenedEvent) {
        inInventory = try {
            checkInventoryName(event.inventoryName)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Failed checking inventory state")
            false
        }

        if (inInventory) {
            try {
                openInventory(event)
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(e, "Failed to run inventory open in InventoryDetector")
            }
        }
    }
}
