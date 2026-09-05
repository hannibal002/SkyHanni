package at.hannibal2.skyhanni.utils.api

import at.hannibal2.skyhanni.SkyHanniMod
import java.net.URI
import java.net.http.HttpRequest
import java.nio.charset.StandardCharsets

/**
 * Represents a static API path that can be used to fetch data from a predefined URL.
 * @param url The URL of the API endpoint.
 * @param apiName The name of the API being requested, used for logging and error handling.
 * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
 */
open class ApiStaticPath(
    open val url: String,
    open val apiName: String,
    open val silentError: Boolean = true,
) {
    fun toGet(tryForceGzip: Boolean = false) = ApiStaticGetPath(url, apiName, silentError, tryForceGzip)
    fun toPost(failOnNoContentLength: Boolean = false) = ApiStaticPostPath(url, apiName, silentError, failOnNoContentLength)

    /**
     * Builds a request for this path, pre-filled with the headers every SkyHanni request shares.
     * @param block Applied to the builder, may override any of the defaults.
     */
    fun buildRequest(block: HttpRequest.Builder.() -> Unit = {}): HttpRequest =
        HttpRequest.newBuilder(URI.create(url))
            .timeout(ApiInternalUtils.requestTimeout)
            .header("User-Agent", SkyHanniMod.userAgent)
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("Accept-Encoding", "gzip")
            .apply(block)
            .build()
}

/**
 * See [ApiStaticPath] for general field definitions.
 * Represents a static API path with a URL and API name, with the intention to GET data from it.
 * @param tryForceGzip If true, the response body will be gzip decoded even if the server does not announce it.
 */
data class ApiStaticGetPath(
    override val url: String,
    override val apiName: String,
    override val silentError: Boolean = true,
    val tryForceGzip: Boolean = false,
) : ApiStaticPath(url, apiName, silentError) {

    fun buildGetRequest(): HttpRequest = buildRequest { GET() }

}

/**
 * See [ApiStaticPath] for general field definitions.
 * Represents a static API path with a URL and API name, with the intention to POST data to it.
 * @param failOnNoContentLength If true, the request will fail if the response does not contain a Content-Length header.
 */
data class ApiStaticPostPath(
    override val url: String,
    override val apiName: String,
    override val silentError: Boolean = true,
    val failOnNoContentLength: Boolean = false,
    val contentType: String = "application/json; charset=UTF-8",
) : ApiStaticPath(url, apiName, silentError) {

    fun buildPostRequest(body: String): HttpRequest = buildRequest {
        header("Content-Type", contentType)
        POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
    }
}
