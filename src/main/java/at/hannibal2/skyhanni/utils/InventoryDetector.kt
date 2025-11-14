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
 * @property onOpenInventory A callback triggered when the given inventory is detected to be open. Optional.
 * @property onCloseInventory A callback triggered when the inventory is closed. Optional.
 * @property checkInventoryName Define what inventory name or names we are looking for.
 */
class InventoryDetector(
    val onOpenInventory: (InventoryFullyOpenedEvent) -> Unit = {},
    val onCloseInventory: (InventoryCloseEvent) -> Unit = {},
    val checkInventoryName: (String) -> Boolean,
) {
    constructor(
        pattern: Pattern,
        onOpenInventory: (InventoryFullyOpenedEvent) -> Unit = {},
        onCloseInventory: (InventoryCloseEvent) -> Unit = {},
    ) : this(
        onOpenInventory,
        onCloseInventory,
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
                it.onCloseInventory(event)
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
                onOpenInventory(event)
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(e, "Failed to run inventory open in InventoryDetector")
            }
        }
    }
}
