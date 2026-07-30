package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi
import at.hannibal2.skyhanni.utils.LorenzVec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GardenPlotApiTest {

    @Test
    fun `plot edges prefer the positive plot`() {
        mapOf(
            LorenzVec(0.0, 10.0, -48.0) to 0,
            LorenzVec(-48.0, 10.0, 0.0) to 0,
            LorenzVec(48.0, 10.0, 0.0) to 3,
            LorenzVec(0.0, 10.0, 48.0) to 4,
            LorenzVec(48.0, 10.0, 48.0) to 8,
        ).forEach { (location, expectedPlotId) ->
            assertEquals(expectedPlotId, GardenPlotApi.getPlot(location)?.id, "location $location")
        }
    }
}
