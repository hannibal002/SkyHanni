package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.StringUtils.equalsIgnoreColor

class DebugDataCollectEvent(private val list: MutableList<String>, private val search: String) : SkyHanniEvent() {

    var empty = true
    private var currentTitle = ""
    private var irrelevant = false

    fun title(title: String) {
        if (currentTitle != "") ErrorManager.skyHanniError(
            "DebugDataCollectEvent duplicate titles and no data in between",
            "current title" to currentTitle,
            "new title" to title,
        )

        currentTitle = title
    }

    fun addIrrelevant(builder: MutableList<String>.() -> Unit) = addIrrelevant(buildList(builder))

    fun addIrrelevant(text: String) = addIrrelevant(listOf(text))

    fun addIrrelevant(text: List<String>) {
        irrelevant = true
        addData(text)
    }

    fun addData(builder: MutableList<String>.() -> Unit) = addData(buildList(builder))

    fun addData(text: String) = addData(listOf(text))

    fun addData(text: List<String>) {
        if (currentTitle == "") error("Title not set")
        writeData(text)
        currentTitle = ""
        irrelevant = false
    }

    private fun writeData(text: List<String>) {
        if (irrelevant && search.isEmpty()) return
        if (search.isNotEmpty()) {
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
