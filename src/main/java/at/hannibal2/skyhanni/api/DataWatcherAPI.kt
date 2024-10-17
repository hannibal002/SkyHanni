package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.events.DataWatcherUpdatedEvent
import at.hannibal2.skyhanni.events.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.api.event.HandleEvent

@SkyHanniModule
object DataWatcherAPI {

    private const val DATA_VALUE_CUSTOM_NAME = 2

    @HandleEvent
    fun onDataWatcherUpdate(event: DataWatcherUpdatedEvent) {
        for (updatedEntry in event.updatedEntries) {
            if (updatedEntry.dataValueId == DATA_VALUE_CUSTOM_NAME) {
                EntityCustomNameUpdateEvent(event.entity.customNameTag, event.entity).post()
            }
        }
    }

    // TODO move EntityHealthUpdateEvent logic from EntityData in here
}
