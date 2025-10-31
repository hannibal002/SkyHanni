package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.GenericHanniEvent
import net.minecraft.entity.Entity
//#if MC < 1.21
import net.minecraft.entity.DataWatcher
//#else
//$$ import net.minecraft.entity.data.DataTracker
//#endif

//#if MC < 1.21
data class DataWatcherUpdatedEvent<T : Entity>(val entity: T, val updatedEntries: List<DataWatcher.WatchableObject>) :
    GenericHanniEvent<T>(entity.javaClass)
//#else
//$$ data class DataWatcherUpdatedEvent<T : Entity>(val entity: T, val updatedEntries: List<DataTracker.Entry<*>>) : GenericHanniEvent<T>(entity.javaClass)
//#endif
