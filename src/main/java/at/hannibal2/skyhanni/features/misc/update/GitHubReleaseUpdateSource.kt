package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.features.About.UpdateStream
import at.hannibal2.skyhanni.utils.StringUtils.toQueryString
import at.hannibal2.skyhanni.utils.VersionConstants
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.system.ModVersion
import at.hannibal2.skyhanni.utils.system.PlatformUtils

/**
 * Pulls releases from the releases of a GitHub repository. Only releases with a jar built for the running Minecraft
 * version are considered.
 */
class GitHubReleaseUpdateSource(owner: String, repository: String) : UpdateSource {
    private val releasesUrl = "https://api.github.com/repos/$owner/$repository/releases"

    /**
     * @param page the one-indexed page to fetch, or null to fetch the first page with the API default page size
     * @throws IllegalStateException if the releases could not be fetched
     */
    suspend fun getReleases(page: Int? = null): List<GitHubRelease> {
        val url = releasesUrl + page?.let {
            mapOf("per_page" to RELEASES_PER_PAGE, "page" to it).toQueryString()
        }.orEmpty()
        val (_, json) = ApiUtils.getJsonResponse(url, apiName = "github").assertSuccessWithData()
            ?: error("Failed to fetch GitHub releases")
        return ConfigManager.gson.fromJson<List<GitHubRelease>>(json)
    }

    override suspend fun checkUpdate(updateStream: UpdateStream): UpdateData? = getReleases().asSequence()
        .filter { it.isIn(updateStream) }
        .sortedByDescending { ModVersion.fromString(it.tagName) }
        .firstOrNull { it.hasDownloadForCurrentVersion() }
        ?.let { UpdateData(it.name ?: it.tagName, ModVersion.fromString(it.tagName), it.htmlUrl) }

    private fun GitHubRelease.isIn(updateStream: UpdateStream): Boolean =
        !draft && (!prerelease || updateStream == UpdateStream.BETA)

    private fun GitHubRelease.hasDownloadForCurrentVersion(): Boolean = assets.orEmpty().any { asset ->
        val name = asset.name ?: return@any false
        if (asset.browserDownloadUrl == null) return@any false
        name.endsWith(".jar") && (VersionConstants.MC_VERSION in name || PlatformUtils.MC_VERSION in name)
    }

    companion object {
        private const val RELEASES_PER_PAGE = 100
    }
}
