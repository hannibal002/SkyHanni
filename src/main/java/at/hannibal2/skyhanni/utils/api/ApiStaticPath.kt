package at.hannibal2.skyhanni.utils.api

import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

/**
 * Represents a static Api path that can be used to fetch data from a predefined URL.
 * @param url The URL of the Api endpoint.
 * @param apiName The name of the Api being requested, used for logging and error handling.
 * @param silentError If true, errors will not be logged unless debugConfig.apiUtilsNeverSilent is true.
 */
open class ApiStaticPath(
    open val url: String,
    open val apiName: String,
    open val silentError: Boolean = true,
) {
    fun toGet(tryForceGzip: Boolean = false) = ApiStaticGetPath(url, apiName, silentError, tryForceGzip)
    fun toPost(failOnNoContentLength: Boolean = false) = ApiStaticPostPath(url, apiName, silentError, failOnNoContentLength)

    inline fun <reified T : HttpRequestBase> buildRequest(block: T.() -> Unit = {}): T {
        val ctor = T::class.java.getConstructor(String::class.java)
        return ctor.newInstance(url).apply(block)
    }
}

/**
 * See [ApiStaticPath] for general field definitions.
 * Represents a static Api path with a URL and Api name, with the intention to GET data from it.
 * @param tryForceGzip If true, the request will attempt to use gzip compression. Only relevant for GET requests.
 */
data class ApiStaticGetPath(
    override val url: String,
    override val apiName: String,
    override val silentError: Boolean = true,
    val tryForceGzip: Boolean = false,
) : ApiStaticPath(url, apiName, silentError) {

    fun buildGetRequest() = super.buildRequest<HttpGet> {
        if (tryForceGzip) addHeader("Accept-Encoding", "gzip")
    }

}

/**
 * See [ApiStaticPath] for general field definitions.
 * Represents a static Api path with a URL and Api name, with the intention to POST data to it.
 * @param failOnNoContentLength If true, the request will fail if the response does not contain a Content-Length header.
 */
data class ApiStaticPostPath(
    override val url: String,
    override val apiName: String,
    override val silentError: Boolean = true,
    val failOnNoContentLength: Boolean = false,
    val contentType: ContentType = ContentType.APPLICATION_JSON,
) : ApiStaticPath(url, apiName, silentError) {

    fun buildPostRequest(body: String) = super.buildRequest<HttpPost> {
        entity = StringEntity(body, contentType)
    }
}
