package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.repo.RepoUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File

class RepositoryReloadEvent(val repoLocation: File, val gson: Gson): LorenzEvent() {
    fun getConstant(constant: String): JsonObject? {
        return RepoUtils.getConstant(repoLocation, constant, gson)
    }
}