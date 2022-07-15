package at.hannibal2.skyhanni.utils

import com.google.gson.JsonObject

class MultiFilter {

    val equals = mutableListOf<String>()
    val startsWith = mutableListOf<String>()
    val endsWith = mutableListOf<String>()
    val contains = mutableListOf<String>()

    fun load(hideNpcSell: JsonObject) {
        equals.clear()
        startsWith.clear()
        endsWith.clear()
        contains.clear()

        fill(hideNpcSell, "equals", equals)
        fill(hideNpcSell, "startsWith", startsWith)
        fill(hideNpcSell, "endsWith", endsWith)
        fill(hideNpcSell, "contains", contains)
    }

    private fun fill(jsonObject: JsonObject, key: String, list: MutableList<String>) {
        if (jsonObject.has(key)) {
            list.addAll(jsonObject[key].asJsonArray.map { it.asString })
        }
    }

    fun match(name: String): Boolean {
        if (equals.contains(name)) return true
        if (startsWith.any { name.startsWith(it) }) return true
        if (endsWith.any { name.endsWith(it) }) return true
        if (contains.any { name.contains(it) }) return true

        return false
    }
}