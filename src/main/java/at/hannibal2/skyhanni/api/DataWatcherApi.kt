package at.hannibal2.skyhanni.api import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ElectionApi.derpy
import at.hannibal2.skyhanni.events.DataWatcherUpdatedEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.boss.WitherEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.entity.ExperienceOrbEntity

@SkyHanniModule
object DataWatcherApi {

    private val ignoredEntities = setOf(
        ArmorStandEntity::class.java,
        ExperienceOrbEntity::class.java,
        ItemEntity::class.java,
        ItemFrameEntity::class.java,
        OtherClientPlayerEntity::class.java,
        ClientPlayerEntity::class.java,
    )

    private const val DATA_VALUE_CUSTOM_NAME = 2
    private const val DATA_VALUE_HEALTH = 6

    @HandleEvent
    fun onDataWatcherUpdate(event: DataWatcherUpdatedEvent<Entity>) {
        for (updatedEntry in event.updatedEntries) {
            //#if MC < 1.21
            //$$ if (updatedEntry.dataValueId == DATA_VALUE_CUSTOM_NAME) {
                //#else
                if (updatedEntry.data == Entity.CUSTOM_NAME) {
                //#endif
                EntityCustomNameUpdateEvent(event.entity, event.entity.customName.formattedTextCompatLessResets()).post()
            }

            //#if MC < 1.21
            //$$ if (updatedEntry.dataValueId == DATA_VALUE_HEALTH) {
            //$$     val health = (updatedEntry.`object` as? Float)?.toInt() ?: continue
                //#else
                if (updatedEntry.data == LivingEntity.HEALTH) {
                val health = (updatedEntry.get() as? Float)?.toInt() ?: continue
                //#endif

                val entity = EntityUtils.getEntityByID(event.entity.id) ?: continue
                if (entity.javaClass in ignoredEntities) continue

                if (event.entity is WitherEntity && health == 300 && event.entity.id < 0) continue
                if (event.entity is LivingEntity) {
                    EntityHealthUpdateEvent(event.entity, health.derpy()).post()
                }
            }
        }
    }
}
