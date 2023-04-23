package at.hannibal2.skyhanni.config.migration

import com.google.gson.JsonElement
import java.lang.reflect.Type

fun interface LoadingAdapter<T> {
    fun adapt(path: ResolutionPath, hierarchy: List<JsonElement?>, type: Type): LoadResult<T>
}