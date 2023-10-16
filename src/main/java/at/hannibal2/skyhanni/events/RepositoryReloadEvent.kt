package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.repo.RepoUtils
import at.hannibal2.skyhanni.test.command.ErrorManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.lang.reflect.Type

class RepositoryReloadEvent(val repoLocation: File, val gson: Gson) : LorenzEvent() {
    fun getConstant(constant: String) = getConstant<JsonObject>(constant)

    inline fun <reified T : Any> getConstant(constant: String, type: Type? = null) = try {
        RepoUtils.getConstant(repoLocation, constant, gson, T::class.java, type)
    } catch (e: Exception) {
        ErrorManager.logError(
            Exception("Repo parsing error while trying to read constant '$constant'", e),
            "Error reading repo data"
        )
        null
    }
}