package at.hannibal2.skyhanni.utils

import com.google.gson.JsonObject

class MultiFilter {

    private val equals = mutableListOf<String>()
    private val startsWith = mutableListOf<String>()
    private val endsWith = mutableListOf<String>()
    private val contains = mutableListOf<String>()
    private val containsWord = mutableListOf<String>()

    fun load(hideNpcSell: JsonObject) {
        equals.clear()
        startsWith.clear()
        endsWith.clear()
        contains.clear()
        containsWord.clear()

        fill(hideNpcSell, "equals", equals)
        fill(hideNpcSell, "startsWith", startsWith)
        fill(hideNpcSell, "endsWith", endsWith)
        fill(hideNpcSell, "contains", contains)
        fill(hideNpcSell, "containsWord", containsWord)
    }

    private fun fill(jsonObject: JsonObject, key: String, list: MutableList<String>) {
        if (jsonObject.has(key)) {
            list.addAll(jsonObject[key].asJsonArray.map { it.asString })
        }
    }

    fun match(string: String): Boolean {
        return matchResult(string) != null
    }

    fun matchResult(string: String): String? {
        var result = equals.find { it == string }
        if (result != null) return result
        result = startsWith.find { string.startsWith(it) }
        if (result != null) return result
        result = endsWith.find { string.endsWith(it) }
        if (result != null) return result
        result = contains.find { string.contains(it) }
        if (result != null) return result
        result = containsWord.find { containsWord(string, it) }
        if (result != null) return result

        return null
    }

    private fun containsWord(message: String, word: String): Boolean =
        message.startsWith("$word ") || message.endsWith(" $word") || message.contains(" $word ")

    fun count(): Int {
        return equals.size + startsWith.size + endsWith.size + contains.size + containsWord.size
    }
}