package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import net.minecraft.entity.Entity
//#if MC < 1.21
import net.minecraft.entity.DataWatcher
//#else
//$$ import net.minecraft.entity.data.DataTracker
//#endif

//#if MC < 1.21
data class DataWatcherUpdatedEvent<T : Entity>(val entity: T, val updatedEntries: List<DataWatcher.WatchableObject>) :
    GenericSkyHanniEvent<T>(entity.javaClass)
//#else
//$$ data class DataWatcherUpdatedEvent<T : Entity>(val entity: T, val updatedEntries: List<DataTracker.Entry<*>>) : GenericSkyHanniEvent<T>(entity.javaClass)
//#endif
