package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.repo.RepoUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File

class RepositoryReloadEvent(val repoLocation: File, val gson: Gson): LorenzEvent() {
    fun getConstant(constant: String) = getConstant<JsonObject>(constant)

    inline fun <reified T : Any> getConstant(constant: String) =
        RepoUtils.getConstant(repoLocation, constant, gson, T::class.java)
}