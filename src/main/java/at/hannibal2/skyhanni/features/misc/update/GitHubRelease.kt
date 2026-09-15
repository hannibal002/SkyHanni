package at.hannibal2.skyhanni.features.misc.update

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * One entry of the `api.github.com/repos/{owner}/{repository}/releases` response.
 */
data class GitHubRelease(
    @Expose @SerializedName("tag_name") val tagName: String,
    @Expose val name: String?,
    @Expose val body: String?,
    @Expose val draft: Boolean,
    @Expose val prerelease: Boolean,
    @Expose val assets: List<Asset>?,
    @Expose @SerializedName("html_url") val htmlUrl: String,
) {
    data class Asset(
        @Expose val name: String?,
        @Expose @SerializedName("browser_download_url") val browserDownloadUrl: String?,
    )
}
