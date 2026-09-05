package at.hannibal2.skyhanni.utils.api

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.JsonElement
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.ProxySelector
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.time.Duration
import java.util.zip.GZIPInputStream
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("InjectDispatcher")
object ApiInternalUtils {

    private val debugConfig get() = SkyHanniMod.feature.dev.debug
    val neverSilent get() = debugConfig.apiUtilsNeverSilent

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
        println("Failed to load keystore. A lot of API requests won't work")
        it.printStackTrace()
    }.getOrNull()

    fun patchHttpsRequest(connection: HttpsURLConnection) = ctx?.let {
        connection.sslSocketFactory = it.socketFactory
    }

    private val connectTimeout: Duration = Duration.ofSeconds(10)
    internal val requestTimeout: Duration = Duration.ofSeconds(30)

    /** Binary downloads are whole repository archives, which take far longer than a single API call. */
    internal val binaryRequestTimeout: Duration = Duration.ofMinutes(5)

    @PublishedApi
    internal val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(connectTimeout)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .apply { ProxySelector.getDefault()?.let { proxy(it) } }
        .build()

    /**
     * Driving logic for fetching a Binary response from the API.
     * @param this The [ApiStaticPath] to fetch the Binary response from.
     * @param file The [File] to save the Binary response to.
     * @return A [BinaryApiResponse] containing the result of the request.
     */
    internal suspend fun ApiStaticPath.internalGetBinaryResponse(file: File): BinaryApiResponse = withBinaryHttpClient(file)

    /**
     * Driving logic for posting a JSON body to the API.
     * @param T The type of [JsonElement] expected in the response.
     * @param this The [ApiStaticPostPath] to post the JSON body to.
     * @param jsonBody The JSON body to post as a String.
     * @return A [JsonApiResponse] containing the result of the request, with the response data as a [JsonElement].
     */
    internal suspend inline fun <reified T : JsonElement> ApiStaticPostPath.internalPostJson(
        jsonBody: String,
    ): JsonApiResponse<T> = withJsonHttpClient(
        responseReader = { it.readJsonResponse(failOnNoContentLength = failOnNoContentLength) },
        requestFactory = { buildPostRequest(jsonBody) },
        requestBody = jsonBody,
    )

    /**
     * Driving logic for fetching a JSON response from the API.
     * @param T The type of [JsonElement] expected in the response.
     * @param this The [ApiStaticGetPath] to fetch the JSON response from.
     * @return A [JsonApiResponse] containing the result of the request.
     */
    @PublishedApi
    internal suspend inline fun <reified T : JsonElement> ApiStaticGetPath.internalGetJsonResponse(): JsonApiResponse<T> =
        withJsonHttpClient(
            responseReader = { it.readJsonResponse(tryForceGzip) },
            requestFactory = { buildGetRequest() },
        )

    // <editor-fold desc="Client Execution Wrappers">
    /**
     * Generic method to execute an API request using the shared [httpClient].
     * Executes the given API intention and returns an [Res] (ApiResponse subtype).
     * If the request fails, the error is logged unless the intention is silent.
     * @param Res The type of ApiResponse expected (e.g., [BinaryApiResponse] or [JsonApiResponse]).
     * @param T The type of data expected in the ApiResponse (e.g., [Long] for Binary responses or [JsonElement] for JSON responses).
     * @param requestFactory Creates the [HttpRequest] for the API request.
     * @param responseReader Reads the response body and returns data of type [T].
     * @param dataConsumer Consumes the data and returns an ApiResponse of type [Res].
     * @param requestBody The body sent with the request, if any, used for error logging only.
     * @param responseFilter Decides whether the response should be handed to the [responseReader] at all.
     * @return An ApiResponse of type [Res] containing the result of the request.
     */
    @PublishedApi
    internal suspend inline fun <Res : ApiResponse<T>, T> ApiStaticPath.withHttpClient(
        crossinline requestFactory: ApiStaticPath.() -> HttpRequest,
        crossinline responseReader: (HttpResponse<InputStream>?) -> T?,
        crossinline dataConsumer: (Boolean, String, T?) -> Res,
        requestBody: String? = null,
        crossinline responseFilter: (HttpResponse<InputStream>) -> HttpResponse<InputStream>? = { it.takeIfSuccessful() },
    ): Res = withContext(Dispatchers.IO) {
        ApiIntentionContext(requestFactory(), this@withHttpClient, requestBody).let { apiIntention ->
            var responseData: T? = null
            runCatching {
                httpClient.send(apiIntention.request, HttpResponse.BodyHandlers.ofInputStream()).let { resp ->
                    apiIntention.response = resp
                    resp.body().use {
                        responseData = responseReader(responseFilter(resp))
                    }
                    val statusCode = resp.statusCode()
                    if (statusCode !in 200..299) throw IOException("Request failed with status code $statusCode")
                    dataConsumer(true, "OK", responseData)
                }
            }.getOrElse { e ->
                val message = e.message ?: "Request to ${apiIntention.apiName} failed"
                if (neverSilent || !apiIntention.silentError) ErrorManager.logErrorWithData(
                    e,
                    message,
                    extraData = apiIntention.collectInterestingFields().toTypedArray(),
                )
                dataConsumer(false, message, responseData)
            }
        }
    }

    /**
     * See [withHttpClient] for general field definitions.
     * Specific to fetching a response expecting a JSON body of some type [T].
     * Executes the given API intention and returns a JsonApiResponse of type [T].
     * @param T The type of JsonElement expected in the ApiResponse.
     * @param this The [ApiStaticPath] to execute on the client.
     * @return A [JsonApiResponse] containing the result of the request.
     */
    @PublishedApi
    internal suspend inline fun <reified T : JsonElement> ApiStaticPath.withJsonHttpClient(
        crossinline responseReader: (HttpResponse<InputStream>?) -> T?,
        crossinline requestFactory: ApiStaticPath.() -> HttpRequest = ApiStaticPath::buildRequest,
        requestBody: String? = null,
    ): JsonApiResponse<T> = withHttpClient(
        requestFactory,
        responseReader,
        ::JsonApiResponse,
        requestBody,
        responseFilter = { it },
    )

    /**
     * See [withHttpClient] for general field definitions.
     * Specific to fetching a Binary response and saving it to a file.
     * Executes the given API intention and returns a BinaryApiResponse.
     * @param this The [ApiStaticPath] to execute on the client.
     * @param file The [File] to save the Binary response to.
     * @return A [BinaryApiResponse] containing the result of the request.
     */
    internal suspend inline fun ApiStaticPath.withBinaryHttpClient(
        file: File,
        crossinline responseReader: (HttpResponse<InputStream>?) -> Long? = { it.readBodyToFile(file) },
        crossinline requestFactory: ApiStaticPath.() -> HttpRequest = {
            buildRequest { timeout(binaryRequestTimeout) }
        },
    ): BinaryApiResponse = withHttpClient(requestFactory, responseReader, ::BinaryApiResponse)

    /**
     * The default filter deciding whether a response carries a body worth reading.
     * @param this The [HttpResponse] to check.
     * @return The response if the status code is in the range 200-299 and a body is present, or null otherwise.
     */
    @PublishedApi
    internal fun HttpResponse<InputStream>.takeIfSuccessful(
        failOnNoContentLength: Boolean = true,
    ): HttpResponse<InputStream>? = takeIf {
        statusCode() in 200..299 && (contentLength != 0L || !failOnNoContentLength)
    }

    /** The announced body length, or -1 if the server did not send a Content-Length header. */
    @PublishedApi
    internal val HttpResponse<*>.contentLength: Long
        get() = headers().firstValueAsLong("content-length").orElse(-1L)

    /**
     * The response body, gzip decoded when applicable. [java.net.http.HttpClient] never decodes content encodings itself.
     * @param forceGzip If true, the body is gzip decoded even if the server did not announce the encoding.
     */
    @PublishedApi
    internal fun HttpResponse<InputStream>.decodedBody(forceGzip: Boolean = false): InputStream {
        val gzipped = forceGzip || headers().firstValue("content-encoding")
            .map { it.equals("gzip", ignoreCase = true) }.orElse(false)
        return if (gzipped) GZIPInputStream(body()) else body()
    }

    /**
     * Reads the response body and writes it to a file.
     * @param this The [HttpResponse] to read from.
     * @param file The [File] to write the content to.
     * @return The number of bytes written to the file, or null if the response is null or an error occurs.
     */
    internal fun HttpResponse<InputStream>?.readBodyToFile(file: File): Long? =
        this?.runCatching {
            decodedBody().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.length()
        }?.getOrNull()

    /**
     * Reads the response body and parses it as a JsonElement.
     * @param this The [HttpResponse] to read from.
     * @param tryForceGzip If true, the body will be gzip decoded even if the server did not announce the encoding.
     * @param failOnNoContentLength If true, the method will return null if the content length is 0.
     * @return A parsed [JsonElement] or null if the content is empty or an error occurs.
     */
    @PublishedApi
    internal inline fun <reified T : JsonElement> HttpResponse<InputStream>?.readJsonResponse(
        tryForceGzip: Boolean = false,
        failOnNoContentLength: Boolean = true,
    ): T? = when {
        this == null || (contentLength == 0L && failOnNoContentLength) -> null
        else -> runCatching {
            val text = decodedBody(tryForceGzip).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            if (text.isBlank()) null
            else ConfigManager.gson.fromJson<T>(text)
        }.getOrNull()
    }
    // </editor-fold>
}
