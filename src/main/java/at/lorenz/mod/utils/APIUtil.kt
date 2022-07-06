package at.lorenz.mod.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils
import scala.util.parsing.json.JSONArray
import scala.util.parsing.json.JSONObject
import java.awt.image.BufferedImage
import java.net.HttpURLConnection
import java.net.URL
import java.security.cert.X509Certificate
import javax.imageio.ImageIO


object APIUtil {
    private val parser = JsonParser()

//    val sslContext = SSLContexts.custom()
//        .loadTrustMaterial { chain, authType ->
//            isValidCert(chain, authType)
//        }
//        .build()
//    val sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
//        .setSslContext(sslContext)
//        .build()

//    val cm = PoolingHttpClientConnectionManagerBuilder.create()
//        .setSSLSocketFactory(sslSocketFactory)

    val builder: HttpClientBuilder =
        HttpClients.custom().setUserAgent("LorenzMod")
//            .setConnectionManagerShared(true)
//            .setConnectionManager(cm.build())
            .setDefaultHeaders(
                mutableListOf(
                    BasicHeader("Pragma", "no-cache"),
                    BasicHeader("Cache-Control", "no-cache")
                )
            )
            .setDefaultRequestConfig(
                RequestConfig.custom()
//                    .setConnectTimeout(Timeout.ofMinutes(1))
//                    .setResponseTimeout(Timeout.ofMinutes(1))
                    .build()
            )
            .useSystemProperties()

    /**
     * Taken from Elementa under MIT License
     * @link https://github.com/Sk1erLLC/Elementa/blob/master/LICENSE
     */
    fun URL.getImage(): BufferedImage {
        val connection = this.openConnection() as HttpURLConnection

        connection.requestMethod = "GET"
        connection.useCaches = true
        connection.addRequestProperty("User-Agent", "LorenzMod")
        connection.doOutput = true

        return ImageIO.read(connection.inputStream)
    }

    fun getJSONResponse(urlString: String): JsonObject {
        val client = builder.build()
        try {
            client.execute(HttpGet(urlString)).use { response ->
                val entity = response.entity
                if (entity != null) {
                    val retSrc = EntityUtils.toString(entity)
                    return parser.parse(retSrc) as JsonObject
                    // parsing JSON
//                    val result = JSONObject(retSrc) //Convert String to JSON Object
//                    val tokenList: JSONArray = result.getJSONArray("names")
//                    val oj: JSONObject = tokenList.getJSONObject(0)
//                    val token: String = oj.getString("name")
                }
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
            LorenzUtils.error("Skytils ran into an ${ex::class.simpleName ?: "error"} whilst fetching a resource. See logs for more details.")
        } finally {
            client.close()
        }
        return JsonObject()
    }

//    fun getArrayResponse(urlString: String): JsonArray {
//        val client = builder.build()
//        try {
//            client.execute(HttpGet(urlString)).use { response ->
////                response.entity.content
//                response.entity.content { entity ->
//                    val obj = parser.parse(EntityUtils.toString(entity)).asJsonArray
//                    EntityUtils.consume(entity)
//                    return obj
//                }
//            }
//        } catch (ex: Throwable) {
//            LorenzUtils.error("Skytils ran into an ${ex::class.simpleName ?: "error"} whilst fetching a resource. See logs for more details.")
//            ex.printStackTrace()
//        } finally {
//            client.close()
//        }
//        return JsonArray()
//    }

    private fun isValidCert(chain: Array<X509Certificate>, authType: String): Boolean {
        return chain.any { it.issuerDN.name == "CN=R3, O=Let's Encrypt, C=US" }
    }
}