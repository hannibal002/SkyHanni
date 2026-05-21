package at.hannibal2.skyhanni.test.event

import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteFeastData
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.TimeProvider
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.utils.json.fromJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class HarvestFeastDataFetchingTest {

    @BeforeEach
    fun setUp() {
        SimpleTimeMark.timeProvider = TimeProvider { MOCK_TIME }
        assertEquals(MOCK_TIME, SimpleTimeMark.now().toMillis(), "Failed to set up time mocking")
    }

    @AfterEach
    fun tearDown() {
        SimpleTimeMark.resetTimeProvider()
    }

    fun assertRemainingTime(jsonString: String, expectedTime: Duration) {
        val feastData = ApiUtils.serializeNullsGson.fromJson<EliteFeastData>(jsonString)
        assertEquals(expectedTime, feastData.getActiveDuration())
    }

    @Test
    fun `current in-season crops have correct remaining time`() = assertRemainingTime(
        """{"complete":true,"current":["Melon","Potato","Sunflower"],"isGrandFeast":false,"month":7,"next":{"Cactus":1779309300,"Carrot":1779309300,"Cocoa Beans":1779272100,"Melon":null,"Moonflower":null,"Mushroom":null,"Nether Wart":1779272100,"Potato":null,"Pumpkin":null,"Sugar Cane":1779309300,"Sunflower":null,"Wheat":null,"Wild Rose":1779272100},"year":491}""",
        34_192.seconds,
    )

    @Test
    fun `current in-season crops have correct remaining time for last rotation`() = assertRemainingTime(
        """{"complete":true,"current":["Melon","Potato","Sunflower"],"isGrandFeast":false,"month":7,"next":{"Cactus":null,"Carrot":null,"Cocoa Beans":null,"Melon":null,"Moonflower":null,"Mushroom":null,"Nether Wart":null,"Potato":null,"Pumpkin":null,"Sugar Cane":null,"Sunflower":null,"Wheat":null,"Wild Rose":null},"year":491}""",
        108_592.seconds,
    )

    companion object {
        const val MOCK_TIME = 1_779_237_908_000L
    }
}
