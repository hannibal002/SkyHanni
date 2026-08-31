package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryDetector

object StarlynSisterDetector {
    fun createStarlynDetector(
        isEnabled: () -> Boolean,
        setSisterType: (StarlynSisterType?) -> Unit,
        onOpen: (event: InventoryFullyOpenedEvent, sister: StarlynSisterType) -> Unit,
        onClose: () -> Unit,
    ): InventoryDetector {
        val sisterTypeMap = StarlynSisterType.entries.associateBy { it.inventoryName }
        var isInventoryOpen: Boolean

        return InventoryDetector(
            checkInventoryName = sisterTypeMap.keys::contains,
            onOpenInventory = { event ->
                if (isEnabled()) {
                    isInventoryOpen = true
                    DelayedRun.runOrNextTick {
                        if (!isInventoryOpen) return@runOrNextTick

                        sisterTypeMap[event.inventoryName]?.let { sister ->
                            setSisterType(sister)
                            onOpen(event, sister)
                        }
                    }
                }
            },
            onCloseInventory = {
                isInventoryOpen = false
                setSisterType(null)
                onClose()
            },
        )
    }
}
