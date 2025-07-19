package at.hannibal2.skyhanni.utils.api

import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addAll
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import java.nio.charset.StandardCharsets

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
internal open class ApiIntentionContext(
    open val url: String,
    open val apiName: String,
    val request: HttpUriRequest,
    open var response: CloseableHttpResponse? = null,
    open val silentError: Boolean,
) {
    constructor(request: HttpUriRequest, path: ApiStaticPath) : this(
        url = path.url,
        apiName = path.apiName,
        request = request,
        silentError = path.silentError,
    )

    constructor(request: HttpUriRequest, apiName: String, silentError: Boolean) : this(
        url = request.uri.toURL().toString(),
        apiName = apiName,
        request = request,
        silentError = silentError,
    )

    /**
     * Collects "interesting" fields related to an Api request, for use in error logging.
     * This includes the Api name, URL, request method, response headers, status, and any post body content.
     * Feel free to add more fields as you need them.
     *
     * @param this The ApiIntentionContext containing the Api request and possibly response data.
     * @return A [List] of pairs where each pair contains a field name and its corresponding value.
     */
    private fun collectInterestingFields(): List<Pair<String, Any?>> = buildList {
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
}

/**
 * See [ApiIntentionContext] for general field definitions.
 * Represents the intention to perform a GET request to an Api endpoint.
 * @param tryForceGzip If true, the GET request will attempt to use gzip compression.
 */
@PublishedApi
internal data class GetApiIntentionContext(
    override val url: String,
    override val apiName: String,
    val getRequest: HttpGet,
    override var response: CloseableHttpResponse? = null,
    override val silentError: Boolean,
    val tryForceGzip: Boolean = false,
) : ApiIntentionContext(url, apiName, getRequest, response, silentError)
