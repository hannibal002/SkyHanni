package at.hannibal2.skyhanni.utils.api

import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addAll
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Represents the intention to perform an API request, and the data associated with it.
 * This class is used to encapsulate the request and response data.
 *
 * @param url The URL of the API request.
 * @param apiName The name of the API being requested.
 * @param request The HTTP request to be executed.
 * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
 * @param requestBody The body sent with the request, if any. [HttpRequest] does not expose it once built.
 */
@PublishedApi
internal class ApiIntentionContext(
    val url: String,
    val apiName: String,
    val request: HttpRequest,
    val silentError: Boolean,
    val requestBody: String? = null,
) {
    var response: HttpResponse<*>? = null

    constructor(request: HttpRequest, path: ApiStaticPath, requestBody: String? = null) : this(
        url = path.url,
        apiName = path.apiName,
        request = request,
        silentError = path.silentError,
        requestBody = requestBody,
    )

    /**
     * Collects "interesting" fields related to an API request, for use in error logging.
     * This includes the API name, URL, request method, response headers, status, and any post body content.
     * Feel free to add more fields as you need them.
     *
     * @param this The ApiIntentionContext containing the API request and possibly response data.
     * @return A [List] of pairs where each pair contains a field name and its corresponding value.
     */
    fun collectInterestingFields(): List<Pair<String, Any?>> = buildList {
        addAll(
            "api name" to apiName,
            "url" to url,
            "request method" to request.method(),
        )
        response?.let { resp ->
            add("response headers" to resp.headers().map().entries.joinToString { "${it.key}: ${it.value.joinToString()}" })
            add("response status code" to resp.statusCode().toString())
        }
        requestBody?.let { body ->
            addAll(
                "post body" to body,
                "content mime type" to request.headers().firstValue("Content-Type").orElse("unknown"),
            )
        }
    }
}
