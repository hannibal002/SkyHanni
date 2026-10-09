package at.hannibal2.skyhanni.config.storage

import com.google.gson.JsonElement
import com.google.gson.annotations.Expose

class EnforcedUserValuesStorage {
    @Expose
    val userValues: MutableMap<String, JsonElement> = mutableMapOf()
}
