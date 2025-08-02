package at.hannibal2.skyhanni.utils.api

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.http.HttpEntity
import org.apache.http.client.HttpResponseException
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.protocol.RequestAcceptEncoding
import org.apache.http.client.protocol.ResponseContentEncoding
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.zip.GZIPInputStream
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

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
        println("Failed to load keystore. A lot of Api requests won't work")
        it.printStackTrace()
    }.getOrNull()

    fun patchHttpsRequest(connection: HttpsURLConnection) = ctx?.let {
        connection.sslSocketFactory = it.socketFactory
    }

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

    /**
     * Driving logic for fetching a Zip response from the Api.
     * @param this The [ApiStaticPath] to fetch the Zip response from.
     * @param file The [File] to save the Zip response to.
     * @return A [ZipApiResponse] containing the result of the request.
     */
    internal suspend fun ApiStaticPath.internalGetZipResponse(file: File): ZipApiResponse = withZipHttpClient<HttpGet>(file)

    /**
     * Driving logic for posting a Json body to the Api.
     * @param T The type of [JsonElement] expected in the response.
     * @param this The [ApiStaticPostPath] to post the Json body to.
     * @param jsonBody The Json body to post as a String.
     * @return A [JsonApiResponse] containing the result of the request, with the response data as a [JsonElement].
     */
    internal suspend inline fun <reified T : JsonElement> ApiStaticPostPath.internalPostJson(
        jsonBody: String,
    ): JsonApiResponse<T> = withJsonHttpClient<T, HttpPost>(
        entityHandler = { it.readEntityJsonResponse(failOnNoContentLength = failOnNoContentLength) },
        requestFactory = { buildPostRequest(jsonBody) },
    )

    /**
     * Driving logic for fetching a Json response from the Api.
     * @param T The type of [JsonElement] expected in the response.
     * @param this The [ApiStaticGetPath] to fetch the Json response from.
     * @return A [JsonApiResponse] containing the result of the request.
     */
    @PublishedApi
    internal suspend inline fun <reified T : JsonElement> ApiStaticGetPath.internalGetJsonResponse(): JsonApiResponse<T> =
        withJsonHttpClient<T, HttpGet>(
            entityHandler = { it.readEntityJsonResponse(tryForceGzip) },
            requestFactory = { buildGetRequest() },
        )

    // <editor-fold desc="Client Execution Wrappers">
    /**
     * Generic method to execute an Api request using the provided HttpClient.
     * Executes the given Api intention and returns an [Res] (ApiResponse subtype).
     * If the request fails, it will call the exceptionHandler with the error.
     * @param Res The type of ApiResponse expected (e.g., [ZipApiResponse] or [JsonApiResponse]).
     * @param T The type of data expected in the ApiResponse (e.g., [Long] for Zip responses or [JsonElement] for Json responses).
     * @param Req The type of HttpRequestBase to be used (e.g., [HttpGet] or [HttpPost]).
     * @param requestFactory Creates the HttpRequestBase for the Api request.
     * @param entityHandler Processes the HttpEntity from the response and returns data of type [T].
     * @param dataConsumer Consumes the data and returns an ApiResponse of type [Res].
     * @param entityGetter Extracts the HttpEntity from the CloseableHttpResponse.
     * @return An ApiResponse of type [Res] containing the result of the request.
     */
    @PublishedApi
    internal suspend inline fun <Res : ApiResponse<T>, T, Req : HttpRequestBase> ApiStaticPath.withHttpClient(
        crossinline requestFactory: ApiStaticPath.() -> Req,
        crossinline entityHandler: (HttpEntity?) -> T?,
        crossinline dataConsumer: (Boolean, String, T?) -> Res,
        crossinline entityGetter: (CloseableHttpResponse) -> HttpEntity? = { it.getEntityOrNull() },
    ): Res = withContext(Dispatchers.IO) {
        ApiIntentionContext(requestFactory(), this@withHttpClient).let { apiIntention ->
            runCatching {
                httpClient.execute(apiIntention.request).use { resp ->
                    if (resp.statusLine.statusCode !in 200..299)
                        throw HttpResponseException(resp.statusLine.statusCode, resp.statusLine.reasonPhrase)
                    apiIntention.response = resp
                    val entity = entityGetter(resp)
                    val data = entityHandler(entity)
                    val message = resp.statusLine?.reasonPhrase ?: "OK"
                    dataConsumer(true, message, data)
                }
            }.getOrElse { e ->
                val message = e.message ?: "Request to ${apiIntention.apiName} failed"
                if (neverSilent || !apiIntention.silentError) ErrorManager.logErrorWithData(
                    e,
                    message,
                    extraData = listOf(
                        "api name" to apiIntention.apiName,
                        "url" to apiIntention.url,
                        "request method" to apiIntention.request.method,
                        "response status" to apiIntention.response?.statusLine.toString(),
                        "response headers" to apiIntention.response?.allHeaders?.joinToString { header ->
                            "${header.name}: ${header.value}"
                        },
                    ).toTypedArray()
                )
                dataConsumer(false, message, null)
            }
        }
    }

    /**
     * See [withHttpClient] for general field definitions.
     * Specific to fetching a response expecting a Json body of some type [T].
     * Executes the given Api intention and returns a JsonApiResponse of type [T].
     * @param T The type of JsonElement expected in the ApiResponse.
     * @param Req The type of HttpRequestBase to be used (e.g., [HttpPost] or [HttpGet]).
     * @param this The [ApiStaticPath] to execute on the client.
     * @return A [JsonApiResponse] containing the result of the request.
     */
    @PublishedApi
    internal suspend inline fun <reified T : JsonElement, reified Req : HttpRequestBase> ApiStaticPath.withJsonHttpClient(
        crossinline entityHandler: (HttpEntity?) -> T?,
        crossinline requestFactory: ApiStaticPath.() -> Req = ApiStaticPath::buildRequest,
    ): JsonApiResponse<T> = withHttpClient(requestFactory, entityHandler, ::JsonApiResponse)

    /**
     * See [withHttpClient] for general field definitions.
     * Specific to fetching a Zip response and saving it to a file.
     * Executes the given Api intention and returns a ZipApiResponse.
     * @param Req The type of HttpRequestBase to be used (e.g., [HttpGet]).
     * @param this The [ApiStaticPath] to execute on the client.
     * @param file The [File] to save the Zip response to.
     * @return A [ZipApiResponse] containing the result of the request.
     */
    internal suspend inline fun <reified Req : HttpRequestBase> ApiStaticPath.withZipHttpClient(
        file: File,
        crossinline entityHandler: (HttpEntity?) -> Long? = { it.readEntityToFile(file) },
        crossinline requestFactory: ApiStaticPath.() -> Req = {
            buildRequest { addHeader("Accept-Encoding", "gzip") }
        }
    ): ZipApiResponse = withHttpClient(requestFactory, entityHandler, ::ZipApiResponse)

    /**
     * The default method to fetch an [HttpEntity] from a [CloseableHttpResponse] (this).
     * @param this The [CloseableHttpResponse] from which to extract the [HttpEntity].
     * @return The [HttpEntity] if the response status code is in the range 200-299, or null if the status code indicates an error.
     */
    @PublishedApi
    internal fun CloseableHttpResponse.getEntityOrNull(
        failOnNoContentLength: Boolean = true,
    ): HttpEntity? = if (this.statusLine.statusCode in 200..299) {
        this.entity.takeIf { it?.contentLength != 0L || !failOnNoContentLength }
    } else null

    /**
     * Reads the content of the HttpEntity and writes it to a file.
     * @param this The [HttpEntity] to read from.
     * @param file The [File] to write the content to.
     * @return The number of bytes written to the file, or null if the entity is null or an error occurs.
     */
    internal fun HttpEntity?.readEntityToFile(file: File): Long? =
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
     * @param this The [HttpEntity] to read from.
     * @param tryForceGzip If true, the content will be read as a GZIP stream.
     * @param failOnNoContentLength If true, the method will return null if the content length is 0.
     * @return A parsed [JsonElement] or null if the content is empty or an error occurs.
     */
    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal inline fun <reified T : JsonElement> HttpEntity?.readEntityJsonResponse(
        tryForceGzip: Boolean = false,
        failOnNoContentLength: Boolean = true,
    ): T? = when {
        this == null || (this.contentLength == 0L && failOnNoContentLength) -> null
        else -> runCatching {
            val raw = if (tryForceGzip) GZIPInputStream(this.content) else this.content
            val text = raw.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            if (text.isBlank()) null
            else ConfigManager.Companion.gson.fromJson<T>(text)
        }.getOrNull()
    }
    // </editor-fold>
}
