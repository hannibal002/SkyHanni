package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.DisabledApiJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import org.apache.http.HttpEntity
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils
import java.security.KeyStore
import java.util.zip.GZIPInputStream
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

@SkyHanniModule
object ApiUtils {

    private val parser = JsonParser()

    private val ctx: SSLContext? = run {
        try {
            val myKeyStore = KeyStore.getInstance("JKS")
            myKeyStore.load(ApiUtils.javaClass.getResourceAsStream("/skyhanni-keystore.jks"), "changeit".toCharArray())
            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            kmf.init(myKeyStore, null)
            tmf.init(myKeyStore)
            SSLContext.getInstance("TLS").apply {
                init(kmf.keyManagers, tmf.trustManagers, null)
            }
        } catch (e: Exception) {
            println("Failed to load keystore. A lot of API requests won't work")
            e.printStackTrace()
            null
        }
    }

    fun patchHttpsRequest(connection: HttpsURLConnection) {
        ctx?.let {
            connection.sslSocketFactory = it.socketFactory
        }
    }

    data class ApiResponse(val success: Boolean, val message: String?, val data: JsonObject)

    private val builder: HttpClientBuilder =
        HttpClients.custom().setUserAgent("SkyHanni/${SkyHanniMod.VERSION}")
            .setDefaultHeaders(
                mutableListOf(
                    BasicHeader("Pragma", "no-cache"),
                    BasicHeader("Cache-Control", "no-cache"),
                ),
            )
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .build(),
            )
            .useSystemProperties()

    /**
     * TODO
     * make suspend
     * use withContext(Dispatchers.IO) { APIUtils.getJSONResponse(url) }.asJsonObject
     */
    fun getJSONResponse(urlString: String, silentError: Boolean = false, apiName: String, gunzip: Boolean = false) =
        getJSONResponseAsElement(urlString, silentError, apiName, gunzip) as JsonObject

    fun getJSONResponseAsElement(
        url: String,
        silentError: Boolean = false,
        apiName: String,
        gunzip: Boolean = false,
    ): JsonElement {
        val client = builder.build()
        try {
            client.execute(HttpGet(url)).use { response ->
                val entity = response.entity
                if (entity != null) {
                    val inputStream = if (gunzip) {
                        GZIPInputStream(entity.content)
                    } else {
                        entity.content
                    }
                    val returnedData = inputStream.bufferedReader().use { it.readText() }
                    try {
                        return parser.parse(returnedData)
                    } catch (e: JsonSyntaxException) {
                        val name = e.javaClass.name
                        val message = "$name: ${e.message}"
                        if (e.message?.contains("Use JsonReader.setLenient(true)") == true) {
                            println("MalformedJsonException: Use JsonReader.setLenient(true)")
                            println(" - getJSONResponse: '$url'")
                            ChatUtils.debug("MalformedJsonException: Use JsonReader.setLenient(true)")
                        } else if (returnedData.contains("<center><h1>502 Bad Gateway</h1></center>")) {
                            ErrorManager.skyHanniError(
                                "Error fetching data from $apiName API!",
                                "error message" to "$message(502 Bad Gateway)",
                                "api name" to apiName,
                                "url" to url,
                                "returned data" to returnedData,
                            )
                        } else {
                            ErrorManager.skyHanniError(
                                "Error fetching data from $apiName API!",
                                "error message" to message,
                                "api name" to apiName,
                                "url" to url,
                                "returned data" to returnedData,
                            )
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            if (silentError) {
                throw e
            }
            val name = e.javaClass.name
            val errorMessage = "$name: ${e.message}"
            ErrorManager.skyHanniError(
                "Error fetching data from $apiName API!",
                "api name" to apiName,
                "error message" to errorMessage,
                "url" to url,
            )
        } finally {
            client.close()
        }
        return JsonObject()
    }

    fun postJSON(url: String, body: String, apiName: String): ApiResponse {
        val client = builder.build()

        try {
            val method = HttpPost(url)
            method.entity = StringEntity(body, ContentType.APPLICATION_JSON)

            client.execute(method).use { response ->
                val status = response.statusLine
                val entity = response.entity

                if (status.statusCode in 200..299) {
                    val data = readResponse(entity)
                    return ApiResponse(true, "Request successful", data)
                }

                val message = "POST request to '$url' returned status ${status.statusCode}"
                ErrorManager.logErrorStateWithData(
                    "Error sending data to $apiName API!",
                    "statusCode is ${status.statusCode}",
                    "error message" to "$message(502 Bad Gateway)",
                    "api name" to apiName,
                    "url" to url,
                    "status code" to status.statusCode,
                    "body" to body,
                )
                return ApiResponse(false, message, JsonObject())
            }
        } catch (throwable: Throwable) {
            ErrorManager.logErrorWithData(
                throwable,
                "Error sending data to $apiName API!",
                "api name" to apiName,
                "url" to url,
                "body" to body,
            )
            return ApiResponse(false, throwable.message, JsonObject())
        } finally {
            client.close()
        }
    }

    private fun readResponse(entity: HttpEntity): JsonObject {
        val retSrc = EntityUtils.toString(entity) ?: return JsonObject()

        try {
            val parsed = parser.parse(retSrc)
            if (parsed.isJsonNull) return JsonObject()

            return parsed as JsonObject
        } catch (_: Throwable) {
            // This causes content types that aren't JSON to be ignored
            return JsonObject()
        }
    }

    fun postJSONIsSuccessful(url: String, body: String, apiName: String): Boolean {
        val response = postJSON(url, body, apiName)

        if (response.success) {
            return true
        }

        ErrorManager.logErrorStateWithData(
            "An error occurred during the $apiName API request!",
            "unsuccessful API response",
            "url" to url,
            "apiName" to apiName,
            "body" to body,
            "message" to response.message,
            "response" to response,
        )

        return false
    }

    private var disabledApis: DisabledApiJson? = null

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        disabledApis = event.getConstant<DisabledApiJson>("misc/DisabledApi")
    }

    fun isMoulberryLowestBinDisabled(): Boolean {
        return disabledApis?.disabledMoulberryLowestBin == true
    }

    fun isHypixelItemsDisabled(): Boolean {
        return disabledApis?.disableHypixelItems == true
    }

    fun isBazaarDisabled(): Boolean {
        return disabledApis?.disabledBazaar == true
    }
}
