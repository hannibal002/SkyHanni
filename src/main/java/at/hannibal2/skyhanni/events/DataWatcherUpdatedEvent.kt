package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.entity.DataWatcher
import net.minecraft.entity.Entity

data class DataWatcherUpdatedEvent(
    val entity: Entity,
    val updatedEntries: List<DataWatcher.WatchableObject>,
) : SkyHanniEvent()
