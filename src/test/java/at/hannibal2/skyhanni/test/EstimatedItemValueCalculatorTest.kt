package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.data.jsonobjects.repo.EndCapData
import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemValueCalculationDataJson
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EstimatedItemValueCalculatorTest {

    @Test
    fun `endcap enchants still use only tier one prices for normal book value`() {
        val data = ItemValueCalculationDataJson(
            alwaysActiveEnchants = emptyMap(),
            onlyTierOnePrices = listOf("turbo_wheat"),
            onlyTierFivePrices = emptyList(),
            endcapEnchants = mapOf(
                "turbo_wheat" to listOf(
                    EndCapData(5, "TURBO_GOURD".toInternalName()),
                    EndCapData(6, "ENCHANTED_TURBO_GOURD".toInternalName()),
                ),
            ),
        )

        assertEquals(
            mapOf("TURBO_WHEAT;1".toInternalName() to 16),
            EstimatedItemValueCalculator.fetchEnchantmentItems(
                enchantments = mapOf("turbo_wheat" to 5),
                internalName = "THEORETICAL_HOE_WHEAT_3".toInternalName(),
                data = data,
                isBazaarItem = { true },
            ),
        )
        assertEquals(
            mapOf(
                "TURBO_GOURD".toInternalName() to 1,
                "TURBO_WHEAT;1".toInternalName() to 16,
            ),
            EstimatedItemValueCalculator.fetchEnchantmentItems(
                enchantments = mapOf("turbo_wheat" to 6),
                internalName = "THEORETICAL_HOE_WHEAT_3".toInternalName(),
                data = data,
                isBazaarItem = { true },
            ),
        )
        assertEquals(
            mapOf(
                "TURBO_GOURD".toInternalName() to 1,
                "ENCHANTED_TURBO_GOURD".toInternalName() to 1,
                "TURBO_WHEAT;1".toInternalName() to 16,
            ),
            EstimatedItemValueCalculator.fetchEnchantmentItems(
                enchantments = mapOf("turbo_wheat" to 7),
                internalName = "THEORETICAL_HOE_WHEAT_3".toInternalName(),
                data = data,
                isBazaarItem = { true },
            ),
        )
    }
}
