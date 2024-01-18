package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.StringUtils.equalsIgnoreColor

class DebugDataCollectEvent(private val list: MutableList<String>, private val search: String?) : LorenzEvent() {

    var empty = true
    private var currentTitle = ""
    private var ignore = false

    fun title(title: String) {
        if (currentTitle != "") error("Title already set: '$currentTitle'")

        currentTitle = title
    }

    fun ignore(block: MutableList<String>.() -> Unit) {
        val list = mutableListOf<String>()
        block(list)
        ignore(list)
    }

    fun ignore(text: String) {
        ignore(listOf(text))
    }

    private fun ignore(text: List<String>) {
        ignore = true
        addData(text)
    }

    fun addData(block: MutableList<String>.() -> Unit) {
        val list = mutableListOf<String>()
        block(list)
        addData(list)
    }

    fun addData(text: String) {
        addData(listOf(text))
    }

    fun addData(text: List<String>) {
        if (currentTitle == "") error("Title not set")
        writeData(text)
        currentTitle = ""
        ignore = false
    }

    private fun writeData(text: List<String>) {
        if (ignore && search == null) return
        search?.let {
            if (!search.equalsIgnoreColor("all")) {
                if (!currentTitle.contains(search, ignoreCase = true)) {
                    return
                }
            }
        }
        empty = false
        list.add("")
        list.add("== $currentTitle ==")
        for (line in text) {
            list.add(" $line")
        }
    }
}
