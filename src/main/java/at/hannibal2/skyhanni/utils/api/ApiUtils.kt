package at.hannibal2.skyhanni.utils.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.DisabledApiJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.api.ApiInternalUtils.internalGetJsonResponse
import at.hannibal2.skyhanni.utils.api.ApiInternalUtils.internalGetZipResponse
import at.hannibal2.skyhanni.utils.api.ApiInternalUtils.internalPostJson
import com.google.gson.JsonElement
import java.io.File

@SkyHanniModule
object ApiUtils {

    // <editor-fold desc="GETs">
    /**
     * Fetches a Zip response from the given static Api path, and saves it to the specified file.
     * @param static The [ApiStaticPath] to fetch the Zip response from.
     * @param file The [File] to save the Zip response to.
     * @return A [ZipApiResponse] containing the result of the request.
     */
    suspend fun getZipResponse(file: File, static: ApiStaticPath): ZipApiResponse = static.internalGetZipResponse(file)

    /**
     * Fetches a Zip response from the given URL, Api name, and saves it to the specified file.
     * Defers to creating an [ApiStaticPath] instance for the URL and Api name.
     * @param file The [File] to save the Zip response to.
     * @param url The URL of the Api endpoint.
     * @param apiName The name of the Api being requested, used for logging and error handling.
     * @param silentError If true, errors will not be logged.
     * @return A [ZipApiResponse] containing the result of the request.
     */
    suspend fun getZipResponse(
        file: File,
        url: String,
        apiName: String,
        silentError: Boolean = true,
    ): ZipApiResponse = getZipResponse(file, ApiStaticPath(url, apiName, silentError))

    /**
     * Fetches a Json response from the given static Api path.
     * @param static The [ApiStaticGetPath] to fetch the Json response from.
     * @return A [JsonElement] containing the Json response data, or null if the request failed or returned no data.
     */
    suspend fun getJsonResponse(static: ApiStaticGetPath): JsonApiResponse<JsonElement> = static.internalGetJsonResponse<JsonElement>()

    /**
     * Fetches a Json response from the given URL and Api name.
     * Wraps the URL and Api name into an [ApiStaticGetPath] instance.
     * @param url The URL of the Api endpoint.
     * @param apiName The name of the Api being requested, used for logging and error handling.
     * @param silentError If true, errors will not be logged.
     * @param tryForceGzip If true, the request will attempt to use gzip compression.
     * @return A [JsonElement] containing the Json response data, or null if the request failed or returned no data.
     */
    suspend fun getJsonResponse(
        url: String,
        apiName: String,
        silentError: Boolean = true,
        tryForceGzip: Boolean = false,
    ): JsonApiResponse<JsonElement> = getJsonResponse(ApiStaticGetPath(url, apiName, silentError, tryForceGzip))

    /**
     * Fetches a typed Json response from the given static Api path.
     * @param T The type of [JsonElement] expected in the response.
     * @return
     */
    suspend inline fun <reified T : JsonElement> getTypedJsonResponse(
        static: ApiStaticGetPath,
    ): JsonApiResponse<T> = static.internalGetJsonResponse<T>()

    /**
     * Fetches a typed Json response from the given URL and Api name.
     * Wraps the URL and Api name into an [ApiStaticGetPath] instance.
     * @param T The type of [JsonElement] expected in the response.
     * @param url The URL of the Api endpoint.
     * @param apiName The name of the Api being requested, used for logging and error handling.
     * @param silentError If true, errors will not be logged.
     * @param tryForceGzip If true, the request will attempt to use gzip compression.
     * @return
     */
    suspend inline fun <reified T : JsonElement> getTypedJsonResponse(
        url: String,
        apiName: String,
        silentError: Boolean = true,
        tryForceGzip: Boolean = false,
    ): JsonApiResponse<T> = ApiStaticGetPath(url, apiName, silentError, tryForceGzip).internalGetJsonResponse<T>()
    // </editor-fold>

    // <editor-fold desc="POSTs">
    /**
     * Posts a Json body to the given static Api path.
     * @param static The [ApiStaticPostPath] to post the Json body to.
     * @param jsonBody The Json body to post as a String.
     * @return A [JsonApiResponse] containing the result of the request, with the response data as a [JsonElement].
     */
    suspend fun postJson(
        static: ApiStaticPostPath,
        jsonBody: String,
    ): JsonApiResponse<JsonElement> = static.internalPostJson(jsonBody)

    /**
     * Posts a Json body to the given URL.
     * Wraps the URL and Api name into an [ApiStaticPostPath] instance.
     * @param url The URL of the Api endpoint.
     * @param jsonBody The Json body to post as a String.
     * @param apiName The name of the Api being requested, used for logging and error handling.
     * @param silentError If true, errors will not be logged.
     * @param failOnNoContentLength If true, the request will fail if the response does not contain a Content-Length header.
     * @return A [JsonApiResponse] containing the result of the request, with the response data as a [JsonElement].
     */
    suspend fun postJson(
        url: String,
        jsonBody: String,
        apiName: String,
        silentError: Boolean = true,
        failOnNoContentLength: Boolean = false,
    ): JsonApiResponse<JsonElement> = ApiStaticPostPath(url, apiName, silentError, failOnNoContentLength).internalPostJson(jsonBody)
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
