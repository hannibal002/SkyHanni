package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets


object APIUtil {
    private val parser = JsonParser()

    val builder: HttpClientBuilder =
        HttpClients.custom().setUserAgent("SkyHanni/${SkyHanniMod.VERSION}")
            .setDefaultHeaders(
                mutableListOf(
                    BasicHeader("Pragma", "no-cache"),
                    BasicHeader("Cache-Control", "no-cache")
                )
            )
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .build()
            )
            .useSystemProperties()

    fun getJSONResponse(urlString: String, silentError: Boolean = false): JsonObject {
        val client = builder.build()
        try {
            client.execute(HttpGet(urlString)).use { response ->
                val entity = response.entity
                if (entity != null) {
                    val retSrc = EntityUtils.toString(entity)
                    return parser.parse(retSrc) as JsonObject
                }
            }
        } catch (throwable: Throwable) {
            if (silentError) {
                throw throwable
            } else {
                throwable.printStackTrace()
                LorenzUtils.error("SkyHanni ran into an ${throwable::class.simpleName ?: "error"} whilst fetching a resource. See logs for more details.")
            }
        } finally {
            client.close()
        }
        return JsonObject()
    }

    fun readFile(file: File): BufferedReader {
        return BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8))
    }
}