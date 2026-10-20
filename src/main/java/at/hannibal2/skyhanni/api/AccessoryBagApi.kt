package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.AccessoryBagUpdateEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object AccessoryBagApi {

    private val patternGroup = RepoPattern.group("api.accessory.bag")

    /**
     * REGEX-TEST: Accessory Bag
     * REGEX-TEST: Accessory Bag (1/2)
     * REGEX-TEST: Accessory Bag (909/394294)
     */
    private val inventoryNamePattern by patternGroup.pattern(
        "inventory.name",
        "Accessory Bag(?: \\(\\d+\\/\\d+\\))?",
    )

    fun isAccessoryBag(inventoryName: String): Boolean = inventoryNamePattern.matches(inventoryName)

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!isAccessoryBag(event.inventoryName)) return

        AccessoryBagUpdateEvent(event.inventoryName, event.inventoryItems).post()
    }
}
