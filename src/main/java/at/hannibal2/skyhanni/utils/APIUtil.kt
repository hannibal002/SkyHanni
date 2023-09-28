package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
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
    private var showApiErrors = false

    private val builder: HttpClientBuilder =
        HttpClients.custom().setUserAgent("SkyHanni/${SkyHanniMod.version}")
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

    fun getJSONResponse(urlString: String, silentError: Boolean = false) =
            getJSONResponseAsElement(urlString, silentError) as JsonObject

    fun getJSONResponseAsElement(urlString: String, silentError: Boolean = false, apiName: String = "Hypixel API"): JsonElement {
        val client = builder.build()
        try {
            client.execute(HttpGet(urlString)).use { response ->
                val entity = response.entity
                if (entity != null) {
                    val retSrc = EntityUtils.toString(entity)
                    try {
                        return parser.parse(retSrc)
                    } catch (e: JsonSyntaxException) {
                        if (e.message?.contains("Use JsonReader.setLenient(true)") == true) {
                            println("MalformedJsonException: Use JsonReader.setLenient(true)")
                            println(" - getJSONResponse: '$urlString'")
                            LorenzUtils.debug("MalformedJsonException: Use JsonReader.setLenient(true)")
                        } else if (retSrc.contains("<center><h1>502 Bad Gateway</h1></center>")) {
                            if (showApiErrors && apiName == "Hypixel API") {
                                LorenzUtils.clickableChat(
                                    "[SkyHanni] Problems with detecting the Hypixel API. §eClick here to hide this message for now.",
                                    "shtogglehypixelapierrors"
                                )
                            }
                            e.printStackTrace()

                        } else {
                            CopyErrorCommand.logError(
                                Error("$apiName error for url: '$urlString'", e),
                                "Failed to load data from $apiName"
                            )
                        }
                    }
                }
            }
        } catch (throwable: Throwable) {
            if (silentError) {
                throw throwable
            } else {
                CopyErrorCommand.logError(
                    Error("$apiName error for url: '$urlString'", throwable),
                    "Failed to load data from $apiName"
                )
            }
        } finally {
            client.close()
        }
        return JsonObject()
    }

    fun postJSONIsSuccessful(urlString: String, body: String, silentError: Boolean = false): Boolean {
        val client = builder.build()
        try {
            val method = HttpPost(urlString)
            method.entity = StringEntity(body, ContentType.APPLICATION_JSON)

            client.execute(method).use { response ->
                val status = response.statusLine

                if (status.statusCode >= 200 || status.statusCode < 300) {
                    return true
                }

                println("POST request to '$urlString' returned status ${status.statusCode}")
                LorenzUtils.error("SkyHanni ran into an error whilst sending data. Status: ${status.statusCode}")

                return false
            }
        } catch (throwable: Throwable) {
            if (silentError) {
                throw throwable
            } else {
                throwable.printStackTrace()
                LorenzUtils.error("SkyHanni ran into an ${throwable::class.simpleName ?: "error"} whilst sending a resource. See logs for more details.")
            }
        } finally {
            client.close()
        }

        return false
    }

    fun readFile(file: File): BufferedReader {
        return BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8))
    }

    fun toggleApiErrorMessages() {
        showApiErrors = !showApiErrors
        LorenzUtils.chat("§e[SkyHanni] Hypixel API error messages " + if (showApiErrors) "§chidden" else "§ashown")
    }
}