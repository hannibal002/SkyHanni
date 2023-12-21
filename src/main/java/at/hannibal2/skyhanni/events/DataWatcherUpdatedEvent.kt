package at.hannibal2.skyhanni.events

import net.minecraft.entity.DataWatcher
import net.minecraft.entity.Entity

data class DataWatcherUpdatedEvent(
    val entity: Entity,
    val updatedEntries: List<DataWatcher.WatchableObject>,
) : LorenzEvent()
