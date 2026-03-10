package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object NeuAbiphoneJson {
    val TYPE: Type = object : TypeToken<Map<String, AbiphoneContactInfo>>() {}.type
}

data class AbiphoneContactInfo(
    @Expose val requirement: List<String>? = listOf(),
    @Expose val island: String?,
    @Expose val x: Int? = 0,
    @Expose val y: Int? = 0,
    @Expose val z: Int? = 0,
    @Expose val callNames: List<String>? = listOf(),
)
