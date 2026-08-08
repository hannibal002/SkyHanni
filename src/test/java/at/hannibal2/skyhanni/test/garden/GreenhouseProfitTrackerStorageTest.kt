package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class GreenhouseProfitTrackerStorageTest {

    @Test
    fun `missing legacy profit tracker data is repaired`() {
        val storage = ProfileSpecificStorage.GardenStorage.GreenHouseStorage(profitTracker = null)

        val repaired = storage.getOrCreateProfitTracker()

        assertSame(repaired, storage.profitTracker)
        assertSame(repaired, storage.getOrCreateProfitTracker())
    }
}
