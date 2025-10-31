package at.hannibal2.hanni.features.commands.tabcomplete

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.chat.TabCompletionEvent
import at.hannibal2.hanni.hannimodule.HanniModule

@HanniModule
object ShowItemTabComplete {

    private val config get() = HanniMod.feature.misc.commands.tabComplete

    // TODO repo
    private val showItemCommands = setOf(
        "show",
        "showitem",
        "showoff",
    )

    private val validSuggestions = listOf(
        "item",
        "helmet",
        "chestplate",
        "leggings",
        "boots",
        "necklace",
        "cloak",
        "belt",
        "bracelet",
        "gloves",
        "pet",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabComplete(event: TabCompletionEvent) {
        if (!config.showItem) return

        if (!showItemCommands.any { event.isCommand(it) }) return
        event.addSuggestions(validSuggestions)
    }

}
