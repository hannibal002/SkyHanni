package at.hannibal2.skyhanni.events

class DebugDataCollectEvent(private val builder: StringBuilder) : LorenzEvent() {

    private var currentTitle = ""

    fun title(title: String) {
        if (currentTitle != "") error("Title already set")

        currentTitle = title
    }

    fun exit(text: String) {
        addData(listOf(text))
    }

    fun addData(text: List<String>) {
        if (currentTitle == "") error("Title not set")

        builder.append("\n$currentTitle\n")
        for (line in text) {
            builder.append(" $line\n")
        }
        currentTitle = ""
    }
}
