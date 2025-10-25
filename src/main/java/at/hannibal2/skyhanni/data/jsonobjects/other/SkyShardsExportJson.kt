package at.hannibal2.skyhanni.data.jsonobjects.other

import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object SkyShardsExportJson {
    val TYPE: Type = object : TypeToken<List<SkyShardsExportData>>() {}.type
}

data class SkyShardsExportData(
    @Expose val name: String,
    @Expose val needed: Int,
    @Expose val source: String?,
)
