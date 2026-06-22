package at.hannibal2.skyhanni.utils.api

import com.google.gson.JsonElement

/**
 * Represents a generic API response that can be used for any API request.
 * @param T The type of data returned by the API request, can be any type.
 * @param success Indicates whether the API request was successful.
 * @param message A message describing the result of the API request, can be null if the request was successful.
 * @param data The data of type [T] returned by the API request, can be null if the request was unsuccessful or if no data was returned.
 * This class is used as a base class for more specific API responses like [BinaryApiResponse] and [JsonApiResponse].
 */
open class ApiResponse<T> (open val success: Boolean, open val message: String?, open var data: T? = null) {

    /**
     * Asserts that the API request was successful.
     */
    fun assertSuccess(): ApiResponse<T>? = if (success) this else null

    /**
     * Asserts that the API request was successful and that data is not null.
     */
    fun assertSuccessWithData(): Pair<ApiResponse<T>, T>? = when {
        !success -> null
        else -> data?.let { this to it }
    }
}

/**
 * See [ApiResponse] for general field definitions.
 * Represents the response from an API request that returns a binary file.
 * @param data The [Long] representing the number of bytes written to the [java.io.File],
 *  can be null if the request was unsuccessful or if no data was returned.
 */
data class BinaryApiResponse(
    override val success: Boolean,
    override val message: String? = null,
    override var data: Long? = null,
) : ApiResponse<Long>(success, message, data)

/**
 * See [ApiResponse] for general field definitions.
 * Represents the response from an API request that returns JSON data.
 */
data class JsonApiResponse<T : JsonElement>(
    override val success: Boolean,
    override val message: String? = null,
    override var data: T? = null
) : ApiResponse<T>(success, message, data)
