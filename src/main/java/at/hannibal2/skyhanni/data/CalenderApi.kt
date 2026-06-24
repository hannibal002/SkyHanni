package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CalenderApi {
    private val group = RepoPattern.group("calenderapi")
    var inCalender = false
        private set

    /**
     * REGEX-TEST: Calendar and Events
     */
    private val calendarGuiPattern by group.pattern(
        "gui",
        "Calendar and Events",
    )

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!calendarGuiPattern.matches(event.inventoryName)) return
        inCalender = true
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (!event.reopenSameName) {
            inCalender = false
        }
    }
}
