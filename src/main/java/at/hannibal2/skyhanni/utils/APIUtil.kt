package at.hannibal2.skyhanni.utils

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils


object APIUtil {
    private val parser = JsonParser()

    val builder: HttpClientBuilder =
        HttpClients.custom().setUserAgent("SkyHanni")
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

    fun getJSONResponse(urlString: String): JsonObject {
        val client = builder.build()
        try {
            client.execute(HttpGet(urlString)).use { response ->
                val entity = response.entity
                if (entity != null) {
                    val retSrc = EntityUtils.toString(entity)
                    return parser.parse(retSrc) as JsonObject
                }
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
            LorenzUtils.error("SkyHanni ran into an ${ex::class.simpleName ?: "error"} whilst fetching a resource. See logs for more details.")
        } finally {
            client.close()
        }
        return JsonObject()
    }
}