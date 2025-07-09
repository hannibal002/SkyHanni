package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.json.getJson
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.lang.reflect.Type

abstract class AbstractRepoReloadEvent(
    val repoFileLocation: File,
    val manager: AbstractRepoManager<*, *>
) : SkyHanniEvent() {
    val gson = manager.getGson()
    val repoName = manager.commonRepoName

    @PublishedApi
    internal fun getConstantFile(fileName: String): JsonObject? =
        File(repoFileLocation, "constants/$fileName.json").getJson()

    inline fun <reified T : Any> getConstant(
        constant: String,
        type: Type? = null,
        gson: Gson = this.gson
    ): T = runCatching {
        manager.setLastConstant(constant)
        if (!repoFileLocation.exists()) manager.throwError("Repo folder does not exist!")
        RepoUtils.getConstant(repoFileLocation, constant, gson, T::class.java, type)
    }.getOrElse { e ->
        if (e is RepoError) throw e
        else manager.throwErrorWithCause("Repo parsing error while trying to read constant '$constant'", e)
    }

    inline fun <reified T : Any> readConstant(
        constant: String,
        gson: Gson = this.gson,
    ): T = runCatching {
        val data = getConstantFile(constant) ?: manager.throwError("$constant failed to load from repo!")
        gson.fromJson<T>(data)
    }.getOrElse { e ->
        if (e is RepoError) throw e
        else manager.throwErrorWithCause("$constant failed to read from repo!", e)
    }
}
