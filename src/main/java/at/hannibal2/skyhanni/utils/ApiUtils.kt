package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.DisabledApiJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addAll
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.http.HttpEntity
import org.apache.http.client.HttpResponseException
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
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.zip.GZIPInputStream
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

@SkyHanniModule
@Suppress("InjectDispatcher")
object ApiUtils {

    /**
     * Represents a generic Api response that can be used for any Api request.
     */
    open class ApiResponse<T> (open val success: Boolean, open val message: String?, open var data: T? = null)

    /**
     * Represents the response from an Api request that returns a ZIP file.
     *
     * @param success Indicates whether the Api request was successful.
     * @param message A message describing the result of the Api request, can be null if the request was successful.
     * @param data The [Long] representing the number of bytes written to the [File],
     *  can be null if the request was unsuccessful or if no data was returned.
     */
    data class ZipApiResponse(
        override val success: Boolean,
        override val message: String? = null,
        override var data: Long? = null,
    ) : ApiResponse<Long>(success, message, data)

    /**
     * Represents the response from an Api request that returns JSON data.
     *
     * @param T The type of data returned by the Api, which must be a subtype of [JsonElement].
     * @param success Indicates whether the Api request was successful.
     * @param message A message describing the result of the Api request, can be null if the request was successful.
     * @param data The data [T] returned by the Api request, can be null if the request was unsuccessful or if no data was returned.
     */
    data class JsonApiResponse<T : JsonElement>(
        override val success: Boolean,
        override val message: String? = null,
        override var data: T? = null
    ) : ApiResponse<T>(success, message, data)

    /**
     * Represents a static Api path with a URL and Api name.
     *
     * @param url The URL of the Api endpoint.
     * @param apiName The name of the Api being requested, used for logging and error handling.
     * @param tryForceGzip If true, the request will attempt to use gzip compression. Only relevant for GET requests.
     */
    data class StaticApiPath(
        val url: String,
        val apiName: String,
        val tryForceGzip: Boolean = false
    )

    private val parser: JsonParser = JsonParser()
    private val debugConfig get() = SkyHanniMod.feature.dev.debug
    private val defaultHeaders = listOf(
        BasicHeader("Pragma", "no-cache"),
        BasicHeader("Cache-Control", "no-cache"),
    )
    private val gatedConnectionConfig = RequestConfig.custom()
        .setConnectTimeout(10_000)
        .setSocketTimeout(30_000)
        .setConnectionRequestTimeout(5_000)
        .build()

    @PublishedApi
    internal val httpClient: CloseableHttpClient = HttpClients.custom()
        .setUserAgent("SkyHanni/${SkyHanniMod.VERSION}-${PlatformUtils.MC_VERSION}")
        .setDefaultHeaders(defaultHeaders)
        .setDefaultRequestConfig(gatedConnectionConfig)
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
     * Represents the intention to perform an Api request, and the data associated with it.
     * This class is used to encapsulate the request and response data.
     *
     * @param url The URL of the Api request.
     * @param apiName The name of the Api being requested.
     * @param request The HTTP request to be executed.
     * @param response The HTTP response received from the Api request, if any.
     */
    @PublishedApi
    internal data class ApiIntentionContext(
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

        fun toFailureZipApiResponse(e: Throwable? = null): ZipApiResponse {
            val message = e?.message ?: "Request to $apiName failed"
            return ZipApiResponse(false, message, null)
        }

        fun <T : JsonElement> toFailureJsonApiResponse(e: Throwable? = null): JsonApiResponse<T> {
            val message = e?.message ?: "Request to $apiName failed"
            return JsonApiResponse(false, message, null)
        }
    }

    // <editor-fold desc="Client Execution Wrappers">
    /**
     * The default exception handler for ZIP Api requests.
     * This function logs the error and returns a failure ApiResponse.
     * Override this function if you want to handle exceptions differently.
     */
    private fun defaultZIPExceptionHandler(
        e: Throwable,
        intentionContext: ApiIntentionContext,
        silentError: Boolean = true
    ): ZipApiResponse {
        val shouldSilentError = if (debugConfig.apiUtilsNeverSilent) false else silentError
        if (!shouldSilentError) ErrorManager.logErrorWithData(
            e,
            e.message ?: "Error fetching data from ${intentionContext.apiName} Api",
            extraData = intentionContext.collectInterestingFields().toTypedArray(),
        )
        return intentionContext.toFailureZipApiResponse(e)
    }

    /**
     * The default exception handler for JSON Api requests.
     * This function logs the error and returns a failure ApiResponse.
     * Override this function if you want to handle exceptions differently.
     *
     * @param e The exception that occurred during the Api request.
     * @param intentionContext The context of the Api request, containing the request and possibly response data.
     * @param silentError If true, the error will not be logged, unless debugConfig.apiUtilsNeverSilent is true.
     * @return An [JsonApiResponse] indicating failure, with the error message and empty data.
     */
    @PublishedApi
    internal fun <T : JsonElement> defaultJSONExceptionHandler(
        e: Throwable,
        intentionContext: ApiIntentionContext,
        silentError: Boolean
    ): JsonApiResponse<T> {
        val shouldSilentError = if (debugConfig.apiUtilsNeverSilent) false else silentError
        if (!shouldSilentError) ErrorManager.logErrorWithData(
            e,
            e.message ?: "Error fetching data from ${intentionContext.apiName} Api",
            extraData = intentionContext.collectInterestingFields().toTypedArray(),
        )
        return intentionContext.toFailureJsonApiResponse(e)
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
     * Generic method to execute an Api request using the provided HttpClient.
     * Executes the given Api intention and returns an [R] (ApiResponse subtype).
     * If the request fails, it will call the exceptionHandler with the error.
     *
     * @param R The type of ApiResponse expected (e.g., [ZipApiResponse] or [JsonApiResponse]).
     * @param T The type of data expected in the ApiResponse (e.g., [Long] for ZIP responses or [JsonElement] for JSON responses).
     * @param apiIntention The Api intention to execute, containing the request and Api name.
     * @param silentError If true, the error will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @param exceptionHandler The function to handle exceptions, must return an ApiResponse of type [R].
     * @return An [R] (ApiResponse subtype) indicating success or failure, with populated data where applicable.
     */
    @PublishedApi
    internal inline fun <R : ApiResponse<T>, T> withHttpClient(
        apiIntention: ApiIntentionContext,
        silentError: Boolean = true,
        exceptionHandler: (Throwable, ApiIntentionContext, Boolean) -> R,
        responseHandler: (CloseableHttpResponse) -> R
    ): R = runCatching {
        httpClient.execute(apiIntention.request).use(responseHandler)
    }.getOrElse { e ->
        exceptionHandler(e, apiIntention, silentError)
    }

    /**
     * Specific to fetching a ZIP response and saving it to a file.
     * Executes the given Api intention and returns a ZipApiResponse.
     * If the request fails, it will call the exceptionHandler with the error.
     *
     * @param file The [File] where the ZIP response will be saved.
     * @param apiIntention The Api intention to execute.
     * @param silentError If true, the error will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @param exceptionHandler The function to handle exceptions, must return a ZipApiResponse.
     * @param entityHandler The function to handle the HttpEntity, must return the number of bytes written to the file or null.
     * @return A [ZipApiResponse] indicating success or failure, with the number of bytes written to the file if successful.
     */
    private fun withZipHttpClient(
        file: File,
        apiIntention: ApiIntentionContext,
        silentError: Boolean = true,
        exceptionHandler: (Throwable, ApiIntentionContext, Boolean) -> ZipApiResponse = ::defaultZIPExceptionHandler,
        entityHandler: (HttpEntity?) -> Long? = { it.readEntityToFile(file) },
    ): ZipApiResponse = withHttpClient(apiIntention, silentError, exceptionHandler) { resp ->
        apiIntention.response = resp
        if (resp.statusLine.statusCode !in 200..299)
            throw HttpResponseException(resp.statusLine.statusCode, resp.statusLine.reasonPhrase)
        val entity = resp.getEntityOrNull()
        val bytesWritten = entityHandler.invoke(entity)
            ?: throw HttpResponseException(resp.statusLine.statusCode, "No content in response")
        ZipApiResponse(true, "OK", bytesWritten)
    }

    /**
     * Specific to fetching a response expecting a JSON body of some type [T].
     * Executes the given Api intention and returns a pair of ApiResponse and HttpEntity.
     * If the request fails, it will call the exceptionHandler with the error.
     *
     * @param T The specific subtype of [JsonElement] you expect (e.g., [JsonObject] or [com.google.gson.JsonArray]).
     * @param apiIntention The Api intention to execute.
     * @param silentError If true, the error will not be logged.
     * @param exceptionHandler The function to handle exceptions, must return an ApiResponse.
     * @param entityHandler The function to handle the HttpEntity, must return a parsed JsonElement or null.
     * @return A [JsonApiResponse] indicating success or failure, with populated data where applicable.
     */
    @PublishedApi
    internal inline fun <reified T : JsonElement> withJsonHttpClient(
        apiIntention: ApiIntentionContext,
        silentError: Boolean = true,
        exceptionHandler: (Throwable, ApiIntentionContext, Boolean) -> JsonApiResponse<T> = ::defaultJSONExceptionHandler,
        entityHandler: (HttpEntity?) -> T? = { it.readEntityJsonResponse() },
    ): JsonApiResponse<T> = withHttpClient(apiIntention, silentError, exceptionHandler) { resp ->
        apiIntention.response = resp
        if (resp.statusLine.statusCode !in 200..299)
            throw HttpResponseException(resp.statusLine.statusCode, resp.statusLine.reasonPhrase)
        val entity = resp.getEntityOrNull()
        val data = entityHandler.invoke(entity)
        JsonApiResponse(true, "OK", data)
    }

    /**
     * The default method to fetch content from an HTTP Response.
     *
     * @param failOnNoContentLength If true, the method will return null if the content length is 0.
     * @return The [HttpEntity] if the response is successful and has content, or null otherwise.
     */
    @PublishedApi
    internal fun CloseableHttpResponse.getEntityOrNull(
        failOnNoContentLength: Boolean = true,
    ): HttpEntity? = if (this.statusLine.statusCode in 200..299) {
        this.entity.takeIf { it.contentLength != 0L || !failOnNoContentLength }
    } else null

    /**
     * Reads the content of the HttpEntity and writes it to a file.
     *
     * @param file The [File] where the content will be written.
     * @return The number of bytes written to the file, or null if the entity is null or has no content.
     */
    private fun HttpEntity?.readEntityToFile(file: File): Long? =
        this?.runCatching {
            content.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.length()
        }?.getOrNull()

    /**
     * Reads the content of the HttpEntity and parses it as a JsonElement.
     *
     * @param T the specific subtype of [JsonElement] you expect (e.g., [JsonObject] or [com.google.gson.JsonArray])
     * @return The parsed [JsonElement] from the entity, or null if the entity is null or has no content.
     */
    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : JsonElement> HttpEntity?.readEntityJsonResponse(
        tryForceGzip: Boolean = false,
    ): T? = when {
        this == null || this.contentLength == 0L -> null
        else -> runCatching {
            val raw = if (tryForceGzip) GZIPInputStream(this.content) else this.content
            val text = raw.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            if (text.isBlank()) null
            else when (val parsed = parser.parse(text)) {
                is JsonNull -> null
                else -> parsed as T
            }
        }.getOrNull()
    }
    // </editor-fold>

    // <editor-fold desc="GETs">
    /**
     * Fetches a ZIP response from the given static Api path, and saves it to the specified file.
     *
     * @param file The [File] where the ZIP response will be saved.
     * @param static The [StaticApiPath] containing the URL, Api name.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     */
    suspend fun getZIPResponse(
        file: File,
        static: StaticApiPath,
        silentError: Boolean = true,
    ): ZipApiResponse = withContext(Dispatchers.IO) {
        internalGetZIPResponse(file, static.url, static.apiName, silentError)
    }

    /**
     * Fetches a ZIP response from the given URL, Api name, and saves it to the specified file.
     *
     * @param file The [File] where the ZIP response will be saved.
     * @param url The URL to fetch the ZIP response from.
     * @param apiName The name of the Api being requested, used for logging and error handling.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     */
    suspend fun getZIPResponse(
        file: File,
        url: String,
        apiName: String,
        silentError: Boolean = true,
    ): ZipApiResponse = withContext(Dispatchers.IO) {
        internalGetZIPResponse(file, url, apiName, silentError)
    }

    /**
     * Driving logic for fetching a ZIP response from the Api.
     * This function executes the HTTP GET request and processes the response.
     *
     * @param url The URL to fetch the ZIP response from.
     */
    private fun internalGetZIPResponse(
        file: File,
        url: String,
        apiName: String,
        silentError: Boolean = true,
    ): ZipApiResponse = withZipHttpClient(
        file,
        ApiIntentionContext(
            HttpGet(url).apply {
                addHeader("Accept-Encoding", "gzip")
            },
            apiName
        ),
        silentError = silentError,
        entityHandler = { it.readEntityToFile(file) },
    )

    /**
     * Fetches a JSON response from the given static Api path.
     * This function is a wrapper around [getJSONResponse] that uses the URL and Api name from the [StaticApiPath].
     *
     * @param static The [StaticApiPath] containing the URL, Api name, and whether to try force gzip compression.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @return A [JsonElement] containing the JSON response, or null if the request failed or returned no content.
     */
    suspend fun getJSONResponse(
        static: StaticApiPath,
        silentError: Boolean = true,
    ): JsonElement? = withContext(Dispatchers.IO) {
        internalGetJSONResponse(static.url, static.apiName, silentError, static.tryForceGzip)
    }

    /**
     * Fetches a JSON response from the given URL and Api name.
     *
     * @param url The URL to fetch the JSON response from.
     * @param apiName The name of the Api being requested, used for logging and error handling.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @param tryForceGzip If true, the request will attempt to use gzip compression.
     * @return A [JsonElement] containing the JSON response, or null if the request failed or returned no content.
     */
    suspend fun getJSONResponse(
        url: String,
        apiName: String,
        silentError: Boolean = true,
        tryForceGzip: Boolean = false,
    ): JsonElement? = withContext(Dispatchers.IO) {
        internalGetJSONResponse(url, apiName, silentError, tryForceGzip)
    }

    /**
     * Fetches a typed JSON response from the given URL and Api name.
     *
     * @param T The specific subtype of [JsonElement] you expect (e.g., [JsonObject] or [com.google.gson.JsonArray]).
     * @param url The URL to fetch the JSON response from.
     * @param apiName The name of the Api being requested, used for logging and error handling.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @param tryForceGzip If true, the request will attempt to use gzip compression.
     * @return A [T] containing the parsed JSON response, or null if the request failed or returned no content.
     */
    suspend inline fun <reified T : JsonElement> getTypedJSONResponse(
        url: String,
        apiName: String,
        silentError: Boolean = true,
        tryForceGzip: Boolean = false,
    ): T? = withContext(Dispatchers.IO) {
        internalGetJSONResponse<T>(url, apiName, silentError, tryForceGzip)
    }

    /**
     * Fetches a typed JSON response from the given static Api path.
     *
     * @param T The specific subtype of [JsonElement] you expect (e.g., [JsonObject] or [com.google.gson.JsonArray]).
     * @param static The [StaticApiPath] containing the URL, Api name, and whether to try force gzip compression.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @return A [T] containing the parsed JSON response, or null if the request failed or returned no content.
     */
    suspend inline fun <reified T : JsonElement> getTypedJSONResponse(
        static: StaticApiPath,
        silentError: Boolean = true,
    ): T? = withContext(Dispatchers.IO) {
        internalGetJSONResponse<T>(static.url, static.apiName, silentError, static.tryForceGzip)
    }

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
    @PublishedApi
    internal inline fun <reified T : JsonElement> internalGetJSONResponse(
        url: String,
        apiName: String,
        silentError: Boolean = true,
        tryForceGzip: Boolean = false,
    ): T? {
        val request = HttpGet(url).apply {
            if (tryForceGzip) addHeader("Accept-Encoding", "gzip")
        }
        val apiResponse = withJsonHttpClient<T>(
            ApiIntentionContext(request, apiName),
            silentError = silentError,
            entityHandler = { it.readEntityJsonResponse(tryForceGzip) }
        )
        // Todo discarding the rest of the constructed ApiIntentionContext, maybe return it and parse
        //  data out elsewhere as needed?
        return apiResponse.data
    }
    // </editor-fold>

    // <editor-fold desc="POSTs">
    /**
     * Posts a JSON body to the given static Api path.
     * This function is a wrapper around [postJSON] that uses the URL and Api name from the [StaticApiPath].
     *
     * @param static The [StaticApiPath] containing the URL, Api name, and whether to try force gzip compression.
     * @param jsonBody The JSON body to post.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @return An [JsonApiResponse] containing the success status, message, and data from the Api response.
     */
    suspend fun postJSON(static: StaticApiPath, jsonBody: String, silentError: Boolean = true): JsonApiResponse<JsonElement> =
        withContext(Dispatchers.IO) { internalPostJSON(static.url, jsonBody, static.apiName, silentError) }

    /**
     * Posts a JSON body to the given URL.
     *
     * @param url The URL to post the JSON body to.
     * @param jsonBody The JSON body to post.
     * @param apiName The name of the Api being requested, used for logging and error handling.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @return An [JsonApiResponse] containing the success status, message, and data from the Api response.
     */
    suspend fun postJSON(url: String, jsonBody: String, apiName: String, silentError: Boolean = true): JsonApiResponse<JsonElement> =
        withContext(Dispatchers.IO) { internalPostJSON(url, jsonBody, apiName, silentError) }

    /**
     * Driving logic for posting a JSON body to the Api.
     * This function executes the HTTP POST request and processes the response.
     *
     * @param url The URL to post the JSON body to.
     * @param jsonBody The JSON body to post.
     * @param apiName The name of the Api being requested, used for logging and error handling.
     * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
     * @return An [JsonApiResponse] containing the success status, message, and data from the Api response.
     */
    @PublishedApi
    internal inline fun <reified T : JsonElement> internalPostJSON(
        url: String,
        jsonBody: String,
        apiName: String,
        silentError: Boolean = true
    ): JsonApiResponse<T> {
        val method = HttpPost(url).apply {
            entity = StringEntity(jsonBody, ContentType.APPLICATION_JSON)
        }
        val apiIntention = ApiIntentionContext(method, apiName)
        return withJsonHttpClient<T>(apiIntention, silentError = silentError)
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
}
