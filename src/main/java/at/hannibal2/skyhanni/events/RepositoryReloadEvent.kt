package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.repo.RepoError
import at.hannibal2.skyhanni.data.repo.RepoUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.lang.reflect.Type

class RepositoryReloadEvent(val repoLocation: File, val gson: Gson) : LorenzEvent() {
    fun getConstant(constant: String) = getConstant<JsonObject>(constant)

    inline fun <reified T : Any> getConstant(constant: String, type: Type? = null): T = try {
        if (!repoLocation.exists()) throw RepoError("Repo folder does not exist!")
        RepoUtils.getConstant(repoLocation, constant, gson, T::class.java, type)
    } catch (e: Exception) {
        throw RepoError("Repo parsing error while trying to read constant '$constant'", e)
    }
}
