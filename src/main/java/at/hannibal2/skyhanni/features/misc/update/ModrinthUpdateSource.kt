package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.StringUtils.toQueryString
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import moe.nea.libautoupdate.JsonUpdateSource
import moe.nea.libautoupdate.UpdateData
import java.util.concurrent.CompletableFuture
import java.util.Locale

class ModrinthUpdateSource(private val projectId: String, private val slug: String) : JsonUpdateSource() {

    override fun getGson(): Gson = ConfigManager.gson

    fun getReleases(includeChangelog: Boolean = false): CompletableFuture<List<ModrinthRelease>?> =
        getJsonFromURL(
            String.format(Locale.ROOT, MODRINTH_API_URL, projectId) + mapOf(
                "loaders" to "fabric",
                "game_versions" to PlatformUtils.MC_VERSION,
                "include_changelog" to includeChangelog,
            ).toQueryString(),
            object : TypeToken<List<ModrinthRelease>>() {}.type,
        )

    override fun checkUpdate(updateStream: String): CompletableFuture<UpdateData?>? =
        getReleases().thenApply { releases ->
            releases?.asSequence()
                ?.filter { it.versionType.updateStream == updateStream }
                ?.maxByOrNull { it.versionNumber }
                ?.let { release ->
                    ModrinthUpdateData(
                        release.versionNumber,
                        "https://modrinth.com/mod/$slug/version/${release.id}",
                    )
                }
        }

    companion object {
        const val MODRINTH_API_URL = "https://api.modrinth.com/v2/project/%s/version"
    }
}
