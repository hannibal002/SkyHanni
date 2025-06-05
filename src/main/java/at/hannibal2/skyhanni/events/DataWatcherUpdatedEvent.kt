package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import net.minecraft.entity.Entity
//#if MC < 1.16
import net.minecraft.entity.DataWatcher
//#else
//$$ import net.minecraft.network.syncher.SynchedEntityData
//#endif

data class DataWatcherUpdatedEvent<T : Entity>(
    val entity: T,
    //#if MC < 1.16
    val updatedEntries: List<DataWatcher.WatchableObject>,
    //#else
    //$$ val updatedEntries: List<SynchedEntityData.DataItem<*>>
    //#endif
) : GenericSkyHanniEvent<T>(entity.javaClass)
