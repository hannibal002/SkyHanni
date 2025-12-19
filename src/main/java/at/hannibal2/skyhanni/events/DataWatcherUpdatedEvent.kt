package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import net.minecraft.world.entity.Entity
//#if MC < 1.21
//$$ import net.minecraft.entity.DataWatcher
//#else
import net.minecraft.network.syncher.SynchedEntityData
//#endif

//#if MC < 1.21
//$$ data class DataWatcherUpdatedEvent<T : Entity>(val entity: T, val updatedEntries: List<DataWatcher.WatchableObject>) :
//$$     GenericSkyHanniEvent<T>(entity.javaClass)
//#else
data class DataWatcherUpdatedEvent<T : Entity>(val entity: T, val updatedEntries: List<SynchedEntityData.DataItem<*>>) : GenericSkyHanniEvent<T>(entity.javaClass)
//#endif
