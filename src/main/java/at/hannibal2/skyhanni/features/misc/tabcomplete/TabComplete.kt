package at.hannibal2.skyhanni.features.misc.tabcomplete

object TabComplete {

    @JvmStatic
    fun handleTabComplete(leftOfCursor: String, originalArray: Array<String>): Array<String>? {
        val splits = leftOfCursor.split(" ")
        if (splits.size > 1) {
            var command = splits.first().lowercase()
            if (command.startsWith("/")) {
                command = command.substring(1)
                customTabComplete(command, originalArray)?.let {
                    return buildResponse(splits, it).toTypedArray()
                }
            }
        }
        return null
    }

    private fun customTabComplete(command: String, originalArray: Array<String>): List<String>? {
        WarpTabComplete.handleTabComplete(command)?.let { return it }
        PlayerTabComplete.handleTabComplete(command, originalArray)?.let { return it }

        return null
    }

    private fun buildResponse(arguments: List<String>, fullResponse: List<String>): List<String> {
        if (arguments.size == 2) {
            val start = arguments[1].lowercase()
            return fullResponse.filter { it.lowercase().startsWith(start) }
        }
        return emptyList()
    }
}
