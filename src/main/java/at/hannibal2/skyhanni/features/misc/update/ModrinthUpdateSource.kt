package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.features.About.UpdateStream
import at.hannibal2.skyhanni.utils.StringUtils.toQueryString
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.system.PlatformUtils

class ModrinthUpdateSource(private val projectId: String, private val slug: String) : UpdateSource {
    /**
     * @throws IllegalStateException if the versions could not be fetched
     */
    suspend fun getReleases(includeChangelog: Boolean = false): List<ModrinthRelease> {
        val gson = ConfigManager.gson
        // Modrinth API expects syntax like loaders=["fabric"]
        val url = "https://api.modrinth.com/v2/project/$projectId/version" + mapOf(
            "loaders" to gson.toJson(listOf("fabric")),
            "game_versions" to gson.toJson(listOf(PlatformUtils.MC_VERSION)),
            "include_changelog" to includeChangelog,
        ).toQueryString()
        val (_, json) = ApiUtils.getJsonResponse(url, apiName = "modrinth").assertSuccessWithData()
            ?: error("Failed to fetch Modrinth versions")
        return gson.fromJson<List<ModrinthRelease>>(json)
    }

    override suspend fun checkUpdate(updateStream: UpdateStream): UpdateData? = getReleases().asSequence()
        .filter { updateStream in it.versionType.allowedUpdateStreams }
        .maxByOrNull { it.versionNumber }
        ?.let {
            UpdateData(
                it.versionNumber.asString,
                it.versionNumber,
                "https://modrinth.com/mod/$slug/version/${it.id}",
            )
        }
}
