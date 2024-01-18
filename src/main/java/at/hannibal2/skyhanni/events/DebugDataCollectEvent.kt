package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.StringUtils.equalsIgnoreColor

class DebugDataCollectEvent(private val builder: StringBuilder, private val search: String?) : LorenzEvent() {

    private var currentTitle = ""
    private var exit = false

    fun title(title: String) {
        if (currentTitle != "") error("Title already set: '$currentTitle'")

        currentTitle = title
    }

    fun exit(text: String) {
        exit = true
        addData(listOf(text))
    }

    fun addData(text: List<String>) {
        if (currentTitle == "") error("Title not set")
        writeData(text)
        currentTitle = ""
        exit = false
    }

    private fun writeData(text: List<String>) {
        if (exit && search == null) return
        search?.let {
            if (!search.equalsIgnoreColor("all")) {
                if (!currentTitle.contains(search, ignoreCase = true)) {
                    return
                }
            }
        }
        builder.append("\n== $currentTitle ==\n")
        for (line in text) {
            builder.append(" $line\n")
        }
    }
}
