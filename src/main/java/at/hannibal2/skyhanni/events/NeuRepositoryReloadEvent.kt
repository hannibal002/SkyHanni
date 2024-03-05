package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.NEUItems.manager
import com.google.gson.JsonObject
import java.io.File

class NeuRepositoryReloadEvent : LorenzEvent() {
    fun getConstant(file: String): JsonObject? {
        return manager.getJsonFromFile(File(manager.repoLocation, "constants/$file.json"))
    }
}
