package at.hannibal2.hanni.events.chat

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.utils.system.PlatformUtils

class TabCompletionEvent(
    val leftOfCursor: String,
    val fullText: String,
    private val originalCompletions: List<String>,
) : HanniEvent() {
    val lastWord = leftOfCursor.substringAfterLast(' ')
    private val additionalSuggestions = mutableSetOf<String>()

    fun addSuggestion(suggestion: String) {
        if (!suggestion.startsWith(lastWord, ignoreCase = true)) return
        val adjustedSuggestion = if (PlatformUtils.IS_LEGACY) suggestion else suggestion.removePrefix("/")
        additionalSuggestions.add(adjustedSuggestion)
    }

    fun addSuggestions(suggestions: Iterable<String>) {
        suggestions.forEach(this::addSuggestion)
    }

    val command = if (leftOfCursor.startsWith("/"))
        leftOfCursor.substring(1).substringBefore(" ").lowercase()
    else ""

    fun isCommand(commandName: String): Boolean {
        return commandName.equals(command, ignoreCase = true)
    }

    fun intoSuggestionArray(): Array<String>? {
        if (additionalSuggestions.isEmpty()) return null
        return (originalCompletions + additionalSuggestions).toTypedArray()
    }
}
