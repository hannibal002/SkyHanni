package at.hannibal2.skyhanni.utils

import com.google.gson.JsonObject

class MultiFilter {

    val equals = mutableListOf<String>()
    val startsWith = mutableListOf<String>()
    val endsWith = mutableListOf<String>()

    fun load(hideNpcSell: JsonObject) {
        equals.clear()
        startsWith.clear()
        endsWith.clear()

        for (element in hideNpcSell["equals"].asJsonArray) {
            equals.add(element.asString)
        }
        for (element in hideNpcSell["startsWith"].asJsonArray) {
            startsWith.add(element.asString)
        }
        for (element in hideNpcSell["endsWith"].asJsonArray) {
            endsWith.add(element.asString)
        }
    }

    fun match(name: String): Boolean {
        if (equals.contains(name)) return true

        if (startsWith.any { name.startsWith(it) }) return true
        if (endsWith.any { name.endsWith(it) }) return true

        return false
    }
}