package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.DisabledApiJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addAll
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.http.HttpEntity
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.RequestAcceptEncoding
import org.apache.http.client.protocol.ResponseContentEncoding
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext

@SkyHanniModule
object ApiUtils {
    data class ApiResponse(val success: Boolean, val message: String?, var data: JsonElement)
    data class StaticApiPath(val url: String, val apiName: String)

    private val parser: JsonParser = JsonParser()
    private val debugConfig get() = SkyHanniMod.feature.dev.debug
    private val httpClient: CloseableHttpClient = HttpClients.custom().setUserAgent("SkyHanni/${SkyHanniMod.VERSION}")
        .setDefaultHeaders(
            listOf(
                BasicHeader("Pragma", "no-cache"),
                BasicHeader("Cache-Control", "no-cache"),
            )
        )
        .setDefaultRequestConfig(RequestConfig.custom().build())
        .useSystemProperties()
        .addInterceptorLast(RequestAcceptEncoding())
        .addInterceptorLast(ResponseContentEncoding())
        .build()

    private val ctx: SSLContext? = runCatching {
        val ks = KeyStore.getInstance("JKS")
        ks.load(
            ApiUtils.javaClass.getResourceAsStream("/skyhanni-keystore.jks"),
            "changeit".toCharArray()
        )
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, null)
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(ks)
        SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, tmf.trustManagers, null)
        }
    }.onFailure {
        println("Failed to load keystore. A lot of Api requests won't work")
        it.printStackTrace()
    }.getOrNull()

    fun patchHttpsRequest(connection: HttpsURLConnection) = ctx?.let {
        connection.sslSocketFactory = it.socketFactory
    }

    /**
     * This is probably superfluous and a bit programmatic, but this function
     * ensures that a suspended block runs on the IO dispatcher if it is not already.
     * This ensures any Api requests made within the block are executed on the IO dispatcher,
     * and disconnects the reliance on using [SkyHanniMod.launchIOCoroutine]
     */
    @Suppress("InjectDispatcher")
    private suspend fun <T> ioIfNeeded(block: suspend () -> T): T =
        if (coroutineContext[ContinuationInterceptor] == Dispatchers.IO) block()
        else withContext(Dispatchers.IO) {
            block()
        }

    /**
     * Represents the intention to perform an Api request, and the data associated with it.
     * This class is used to encapsulate the request and response data.
     *
     * @param url The URL of the Api request.
     * @param apiName The name of the Api being requested.
     * @param request The HTTP request to be executed.
     * @param response The HTTP response received from the Api request, if any.
     */
    private data class ApiIntentionContext(
        val url: String,
        val apiName: String,
        val request: HttpUriRequest,
        var response: CloseableHttpResponse? = null
    ) {
        constructor(request: HttpUriRequest, apiName: String) : this(
            url = request.uri.toURL().toString(),
            apiName = apiName,
            request = request,
        )

        fun toFailureApiResponse(e: Throwable? = null): ApiResponse {
            val message = e?.message ?: "Request to $apiName failed"
            return ApiResponse(false, message, JsonObject())
        }
    }

    // <editor-fold desc="Client Execution Wrappers">
    /**
     * The default exception handler for Api requests.
     * This function logs the error and returns a failure ApiResponse.
     * Override this function if you want to handle exceptions differently.
     *
     * @param e The exception that occurred during the Api request.
     * @param intentionContext The context of the Api request, containing the request and possibly response data.
     * @param silentError If true, the error will not be logged, unless debugConfig.apiUtilsNeverSilent is true.
     * @return An [ApiResponse] indicating failure, with the error message and empty data.
     */
    private fun defaultExceptionHandler(
        e: Throwable,
        intentionContext: ApiIntentionContext,
        silentError: Boolean
    ): ApiResponse {
        val shouldSilentError = if (debugConfig.apiUtilsNeverSilent) false else silentError
        if (!shouldSilentError) ErrorManager.logErrorWithData(
            e,
            e.message ?: "Error fetching data from ${intentionContext.apiName} Api",
            extraData = intentionContext.collectInterestingFields().toTypedArray(),
        )
        return intentionContext.toFailureApiResponse(e)
    }

    /**
     * Collects "interesting" fields related to an Api request, for use in error logging.
     * This includes the Api name, URL, request method, response headers, status, and any post body content.
     * Feel free to add more fields as you need them.
     *
     * @param this The ApiIntentionContext containing the Api request and possibly response data.
     * @return A [List] of pairs where each pair contains a field name and its corresponding value.
     */
    private fun ApiIntentionContext.collectInterestingFields(): List<Pair<String, Any?>> = buildList {
        addAll(
            "api name" to apiName,
            "url" to url,
            "request method" to request.method,
        )
        response?.let { resp ->
            add("response headers" to resp.allHeaders.joinToString { "${it.name}: ${it.value}" })
            add("response status" to resp.statusLine.toString())
            add("response status code" to resp.statusLine.statusCode.toString())
        }
        if (request is HttpPost && request.entity != null) {
            val parsedContent = EntityUtils.toString(request.entity, StandardCharsets.UTF_8)
                ?: "No content in request entity"
            val contentType = ContentType.get(request.entity).mimeType
            addAll(
                "post body" to parsedContent,
                "content mime type" to contentType,
            )
        }
    }

    /**
     * Executes the given Api intention and returns a pair of ApiResponse and HttpEntity.
     * If the request fails, it will call the exceptionHandler with the error.
     *
     * @param apiIntention The Api intention to execute.
     * @param silentError If true, the error will not be logged.
     * @param exceptionHandler The function to handle exceptions, must return an ApiResponse.
     * @param responseHandler The function to handle the response, must return an HttpEntity or null.
     * @return A [Pair] of [ApiResponse] and [HttpEntity], where the latter can be null if the request failed.
     */
    private fun withHttpClient(
        apiIntention: ApiIntentionContext,
        silentError: Boolean = true,
        exceptionHandler: (Throwable, ApiIntentionContext, Boolean) -> ApiResponse = ::defaultExceptionHandler,
        responseHandler: (CloseableHttpResponse) -> HttpEntity? = { it.getEntityOrNull() },
    ): Pair<ApiResponse, HttpEntity?> = runCatching {
        val resp = httpClient.execute(apiIntention.request)
            ?: throw IllegalStateException("No response from Api request to ${apiIntention.apiName}")
        val apiResponse = ApiResponse(true, "OK", JsonObject())
        apiResponse to resp.use(responseHandler)
    }.getOrElse { e ->
        exceptionHandler(e, apiIntention, silentError) to null
    }

    /**
     * The default method to fetch content from an HTTP Response.
     *
     * @param failOnNoContentLength If true, the method will return null if the content length is 0.
     * @return The [HttpEntity] if the response is successful and has content, or null otherwise.
     */
    private fun CloseableHttpResponse.getEntityOrNull(
        failOnNoContentLength: Boolean = true,
    ): HttpEntity? = if (this.statusLine.statusCode in 200..299) {
        this.entity.takeIf { it.contentLength > 0 || !failOnNoContentLength }
    } else null

    /**
     * Reads the content of the HttpEntity and parses it as a JsonElement.
     * If the entity is null or has no content, it returns the default value.
     *
     * @param T the specific subtype of [JsonElement] you expect (e.g., [JsonObject] or [com.google.gson.JsonArray])
     * @param default The default value to return if the entity is null or has no content.
     * @return The parsed [JsonElement] from the entity, or the [default] value if the entity is null or empty.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : JsonElement> HttpEntity?.readEntityResponse(
        default: T = JsonObject() as T
    ): T = when {
        this == null || this.contentLength == 0L -> default
        else -> runCatching {
            val text = EntityUtils.toString(this, StandardCharsets.UTF_8)
            val parsed = parser.parse(text)
            if (parsed.isJsonNull) default else parsed as T
        }.getOrDefault(default)
    }
    // </editor-fold>

    // <editor-fold desc="GETs">
    /**
     * Fetches a JSON response from the given static Api path.
     * This function is a wrapper around [getJSONResponse] that uses the URL and Api name from the [StaticApiPath].
     *
     * @param static The [StaticApiPath] containing the URL and Api name.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @return A [JsonObject] containing the JSON response, or null if the request failed or returned no content.
     */
    suspend fun getJSONResponse(static: StaticApiPath, silentError: Boolean = true): JsonObject? =
        ioIfNeeded { internalGetJSONResponse(static.url, static.apiName, silentError) }

    /**
     * Fetches a JSON response from the given URL and Api name.
     *
     * @param url The URL to fetch the JSON response from.
     * @param apiName The name of the Api being requested, used for logging and error handling.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @return A [JsonObject] containing the JSON response, or null if the request failed or returned no content.
     */
    suspend fun getJSONResponse(url: String, apiName: String, silentError: Boolean = true): JsonObject? =
        ioIfNeeded { internalGetJSONResponse(url, apiName, silentError) }

    /**
     * Fetches a typed JSON response from the given URL and Api name.
     *
     * @param T The specific subtype of [JsonElement] you expect (e.g., [JsonObject] or [com.google.gson.JsonArray]).
     * @param url The URL to fetch the JSON response from.
     * @param apiName The name of the Api being requested, used for logging and error handling.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @return A [T] containing the parsed JSON response, or null if the request failed or returned no content.
     */
    suspend fun <T : JsonElement> getTypedJSONResponse(
        url: String,
        apiName: String,
        silentError: Boolean = true
    ): T? = ioIfNeeded { internalGetJSONResponse(url, apiName, silentError) }

    /**
     * Driving logic for fetching a JSON response from the Api.
     * This function executes the HTTP GET request and processes the response.
     *
     * @param T The specific subtype of [JsonElement] you expect (e.g., [JsonObject] or [com.google.gson.JsonArray]).
     * @param url The URL to fetch the JSON response from.
     * @param apiName The name of the Api being requested, used for logging and error handling.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @return A [T] containing the parsed JSON response, or null if the request failed or returned no content.
     */
    private fun <T : JsonElement> internalGetJSONResponse(url: String, apiName: String, silentError: Boolean = true): T? {
        val apiIntention = ApiIntentionContext(HttpGet(url), apiName)
        val (_, entity) = withHttpClient(apiIntention, silentError = silentError)
        return entity.readEntityResponse()
    }
    // </editor-fold>

    // <editor-fold desc="POSTs">
    /**
     * Posts a JSON body to the given static Api path.
     * This function is a wrapper around [postJSON] that uses the URL and Api name from the [StaticApiPath].
     *
     * @param static The [StaticApiPath] containing the URL and Api name.
     * @param jsonBody The JSON body to post.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @return An [ApiResponse] containing the success status, message, and data from the Api response.
     */
    suspend fun postJSON(static: StaticApiPath, jsonBody: String, silentError: Boolean = true): ApiResponse =
        ioIfNeeded { internalPostJSON(static.url, jsonBody, static.apiName, silentError) }

    /**
     * Posts a JSON body to the given URL.
     *
     * @param url The URL to post the JSON body to.
     * @param jsonBody The JSON body to post.
     * @param apiName The name of the Api being requested, used for logging and error handling.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @return An [ApiResponse] containing the success status, message, and data from the Api response.
     */
    suspend fun postJSON(url: String, jsonBody: String, apiName: String, silentError: Boolean = true): ApiResponse =
        ioIfNeeded { internalPostJSON(url, jsonBody, apiName, silentError) }

    /**
     * Driving logic for posting a JSON body to the Api.
     * This function executes the HTTP POST request and processes the response.
     *
     * @param url The URL to post the JSON body to.
     * @param jsonBody The JSON body to post.
     * @param apiName The name of the Api being requested, used for logging and error handling.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @return An [ApiResponse] containing the success status, message, and data from the Api response.
     */
    private fun internalPostJSON(url: String, jsonBody: String, apiName: String, silentError: Boolean = true): ApiResponse {
        val method = HttpPost(url).apply {
            entity = StringEntity(jsonBody, ContentType.APPLICATION_JSON)
        }
        val apiIntention = ApiIntentionContext(method, apiName)
        val (apiResponse, entity) = withHttpClient(apiIntention, silentError = silentError)
        apiResponse.data = entity.readEntityResponse<JsonObject>()

        return apiResponse
    }
    // </editor-fold>

    private var disabledApis: DisabledApiJson? = null

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        disabledApis = event.getConstant<DisabledApiJson>("misc/DisabledApi")
    }

    fun isMoulberryLowestBinDisabled() = disabledApis?.disabledMoulberryLowestBin == true
    fun isHypixelItemsDisabled() = disabledApis?.disableHypixelItems == true
    fun isBazaarDisabled() = disabledApis?.disabledBazaar == true
    fun isEliteAhDisabled() = disabledApis?.disabledEliteAh == true
    fun isEliteBzDisabled() = disabledApis?.disabledEliteBz == true
    fun isEliteItemsDisabled() = disabledApis?.disabledEliteItems == true
}
