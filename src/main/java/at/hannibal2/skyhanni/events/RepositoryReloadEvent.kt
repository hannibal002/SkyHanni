package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.repo.RepoUtils
import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File

class RepositoryReloadEvent(val repoLocation: File, val gson: Gson) : LorenzEvent() {
    fun getConstant(constant: String) = getConstant<JsonObject>(constant)

    inline fun <reified T : Any> getConstant(constant: String) = try {
        RepoUtils.getConstant(repoLocation, constant, gson, T::class.java)
    } catch (e: Exception) {
        CopyErrorCommand.logError(
            Exception("Repo parsing error while trying to read constant '$constant'", e),
            "Error reading repo data"
        )
        null
    }
}