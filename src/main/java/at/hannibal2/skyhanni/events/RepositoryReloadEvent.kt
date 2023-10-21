package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.repo.RepoError
import at.hannibal2.skyhanni.data.repo.RepoManager
import at.hannibal2.skyhanni.data.repo.RepoUtils
import com.google.gson.Gson
import java.io.File
import java.lang.reflect.Type

class RepositoryReloadEvent(val repoLocation: File, val gson: Gson) : LorenzEvent() {

    inline fun <reified T : Any> getConstant(constant: String, type: Type? = null): T = try {
        RepoManager.setlastConstant(constant)
        if (!repoLocation.exists()) throw RepoError("Repo folder does not exist!")
        RepoUtils.getConstant(repoLocation, constant, gson, T::class.java, type)
    } catch (e: Exception) {
        throw RepoError("Repo parsing error while trying to read constant '$constant'", e)
    }
}
