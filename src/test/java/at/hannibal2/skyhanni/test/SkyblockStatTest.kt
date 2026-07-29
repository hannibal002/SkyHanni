package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.data.model.SkyblockStat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SkyblockStatTest {

    @Test
    fun `resolves stats by display name`() {
        Assertions.assertEquals(SkyblockStat.STRENGTH, SkyblockStat.getValueByDisplayNameOrNull("Strength"))
        Assertions.assertEquals(SkyblockStat.MELON_FORTUNE, SkyblockStat.getValueByDisplayNameOrNull("Melon Slice Fortune"))
        Assertions.assertEquals(SkyblockStat.NETHER_STALK_FORTUNE, SkyblockStat.getValueByDisplayNameOrNull("Nether Wart Fortune"))
    }
}
