package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.data.OwnInventoryData
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OwnInventoryDataTest {

    private val helmet = "TIKI_MASK".toInternalName()

    @Test
    fun `normal inventory increase counts as item add`() {
        assertEquals(
            mapOf(helmet to 1),
            OwnInventoryData.calculateAddedItems(
                snapshot(),
                snapshot(inventoryItems = mapOf(helmet to 1)),
            ),
        )
    }

    @Test
    fun `item entering armor slot does not count as item add`() {
        assertEquals(
            emptyMap<NeuInternalName, Int>(),
            OwnInventoryData.calculateAddedItems(
                snapshot(),
                snapshot(armorItems = mapOf(helmet to 1)),
            ),
        )
    }

    @Test
    fun `item moving from inventory to armor slot does not count as item add`() {
        assertEquals(
            emptyMap<NeuInternalName, Int>(),
            OwnInventoryData.calculateAddedItems(
                snapshot(inventoryItems = mapOf(helmet to 1)),
                snapshot(armorItems = mapOf(helmet to 1)),
            ),
        )
    }

    @Test
    fun `item moving from armor slot to inventory does not count as item add`() {
        assertEquals(
            emptyMap<NeuInternalName, Int>(),
            OwnInventoryData.calculateAddedItems(
                snapshot(armorItems = mapOf(helmet to 1)),
                snapshot(inventoryItems = mapOf(helmet to 1)),
            ),
        )
    }

    @Test
    fun `armor slot movement only suppresses matching inventory increase`() {
        assertEquals(
            mapOf(helmet to 1),
            OwnInventoryData.calculateAddedItems(
                snapshot(armorItems = mapOf(helmet to 1)),
                snapshot(inventoryItems = mapOf(helmet to 2)),
            ),
        )
    }

    private fun snapshot(
        inventoryItems: Map<NeuInternalName, Int> = emptyMap(),
        armorItems: Map<NeuInternalName, Int> = emptyMap(),
    ) = OwnInventoryData.InventorySnapshot(inventoryItems, armorItems)
}
