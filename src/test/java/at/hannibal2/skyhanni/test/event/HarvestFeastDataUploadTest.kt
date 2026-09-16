package at.hannibal2.skyhanni.test.event

import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteFeastJson
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.api.ApiUtils
import com.google.gson.JsonParser
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

class HarvestFeastDataUploadTest {

    @Test
    fun `harvest feast submit preserves null next crops in request body`() = runBlocking {
        var capturedRequestBody: String? = null
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            capturedRequestBody = exchange.requestBody.use {
                it.readBytes().toString(StandardCharsets.UTF_8)
            }
            val responseBytes = SAMPLE_RESPONSE.toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, responseBytes.size.toLong())
            exchange.responseBody.use { it.write(responseBytes) }
        }

        try {
            server.start()
            val requestBody = fakeHarvestFeastData().getBody()
            val response = ApiUtils.postJson(
                url = "http://127.0.0.1:${server.address.port}/",
                jsonBody = requestBody,
                apiName = "test harvest feast upload",
            )

            assertTrue(response.success)
            val actualRequestBody = capturedRequestBody ?: error("Expected server to capture request body")
            val actualJson = JsonParser.parseString(actualRequestBody).asJsonObject
            assertEquals(JsonParser.parseString(requestBody), actualJson)
            assertEquals(JsonParser.parseString(EXPECTED_REQUEST_BODY), actualJson)

            val next = actualJson.getAsJsonObject("next")
            assertEquals(CropType.entries.size, next.size())
            for (crop in CURRENT_CROPS) {
                assertTrue(next.has(crop.cropName), "Expected ${crop.cropName} to be sent")
                assertTrue(next.get(crop.cropName).isJsonNull, "Expected ${crop.cropName} to be null")
            }
        } finally {
            server.stop(0)
        }
    }

    private fun fakeHarvestFeastData(): EliteFeastJson {
        val next = CropType.entries.mapIndexed { index, crop ->
            val nextTime = if (crop in CURRENT_CROPS) {
                null
            } else {
                SimpleTimeMark.fromUnixSeconds(FIRST_FAKE_CROP_TIME + index * 3_600)
            }
            crop.cropName to nextTime
        }.toMap()
        return EliteFeastJson.of(
            current = CURRENT_CROPS.map { it.cropName },
            next = next,
            isGrandFeast = true,
        )
    }

    companion object {
        private const val FIRST_FAKE_CROP_TIME = 1_780_000_000L
        private val CURRENT_CROPS = listOf(CropType.MOONFLOWER, CropType.SUGAR_CANE, CropType.MELON)
        private const val SAMPLE_RESPONSE = """{"success":true}"""
        private const val EXPECTED_REQUEST_BODY =
            """{"current":["Moonflower","Sugar Cane","Melon Slice"],"next":{"Wheat":1780000000,"Carrot":1780003600,"Potato":1780007200,"Nether Wart":1780010800,"Pumpkin":1780014400,"Melon Slice":null,"Cocoa Beans":1780021600,"Sugar Cane":null,"Cactus":1780028800,"Mushroom":1780032400,"Sunflower":1780036000,"Moonflower":null,"Wild Rose":1780043200},"isGrandFeast":true}"""
    }
}
