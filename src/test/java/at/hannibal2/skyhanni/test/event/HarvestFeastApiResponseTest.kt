package at.hannibal2.skyhanni.test.event

import at.hannibal2.skyhanni.utils.api.ApiUtils
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

class HarvestFeastApiResponseTest {

    @Test
    fun `failed harvest feast submit keeps server response in json api response data`() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            exchange.requestBody.use { it.readBytes() }
            val responseBytes = SAMPLE_RESPONSE.toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(400, responseBytes.size.toLong())
            exchange.responseBody.use { it.write(responseBytes) }
        }

        try {
            server.start()
            val response = ApiUtils.postJson(
                url = "http://127.0.0.1:${server.address.port}/",
                jsonBody = SAMPLE_REQUEST,
                apiName = "test harvest feast",
            )

            assertFalse(response.success)
            assertEquals(SAMPLE_RESPONSE, response.data?.toString())
        } finally {
            server.stop(0)
        }
    }

    companion object {
        private const val SAMPLE_REQUEST =
            """{"current":["Moonflower","Sugar Cane","Melon Slice"],"next":{"Wheat":1780611300,"Carrot":1780722900,"Potato":1780611300,"Nether Wart":1780611300,"Pumpkin":1780722900,"Cocoa Beans":1780648500,"Cactus":1780908900,"Mushroom":1780722900,"Sunflower":1780685700,"Wild Rose":1780648500},"isGrandFeast":true}"""
        private const val SAMPLE_RESPONSE =
            """{"statusCode":400,"message":"One or more errors occurred!","errors":{"generalErrors":["Invalid number of next season crops! Expected 13, got 10"]}}"""
    }
}
