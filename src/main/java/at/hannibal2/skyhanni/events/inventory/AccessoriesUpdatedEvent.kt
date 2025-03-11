package at.hannibal2.skyhanni.events.inventory

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage

class AccessoriesUpdatedEvent(accessories: ProfileSpecificStorage.StatsStorage.AccessoryStorage) : SkyHanniEvent()
