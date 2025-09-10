package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.TabCompletionEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object ShowItemTabComplete {

    private val config get() = SkyHanniMod.feature.misc.commands.tabComplete

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
