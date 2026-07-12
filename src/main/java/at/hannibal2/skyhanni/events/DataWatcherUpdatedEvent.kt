package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.Thread
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.Entity

@Thread(RENDER)
data class DataWatcherUpdatedEvent<T : Entity>(val entity: T, val updatedEntries: List<SynchedEntityData.DataItem<*>>) :
    GenericSkyHanniEvent<T>(entity.javaClass)
