package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.StringUtils.toQueryString
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import moe.nea.libautoupdate.JsonUpdateSource
import moe.nea.libautoupdate.UpdateData
import java.util.concurrent.CompletableFuture

class ModrinthUpdateSource(private val projectId: String, private val slug: String) : JsonUpdateSource() {

    override fun getGson(): Gson = ConfigManager.gson

    fun getReleases(includeChangelog: Boolean = false): CompletableFuture<List<ModrinthRelease>?> =
        // Modrinth API expects syntax like loaders=["fabric"]
        getJsonFromURL(
            "https://api.modrinth.com/v2/project/$projectId/version" + mapOf(
                "loaders" to gson.toJson(listOf("fabric")),
                "game_versions" to gson.toJson(listOf(PlatformUtils.MC_VERSION)),
                "include_changelog" to includeChangelog,
            ).toQueryString(),
            object : TypeToken<List<ModrinthRelease>>() {}.type,
        )

    override fun checkUpdate(updateStream: String): CompletableFuture<UpdateData?>? =
        getReleases().thenApply { releases ->
            releases?.asSequence()
                ?.filter { updateStream in it.versionType.allowedUpdateStreams }
                ?.maxByOrNull { it.versionNumber }
                ?.let { release ->
                    ModrinthUpdateData(
                        release.versionNumber,
                        "https://modrinth.com/mod/$slug/version/${release.id}",
                    )
                }
        }
}
