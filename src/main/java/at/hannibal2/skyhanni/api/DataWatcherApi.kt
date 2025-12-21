package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ElectionApi.derpy
import at.hannibal2.skyhanni.events.DataWatcherUpdatedEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity

@SkyHanniModule
object DataWatcherApi {

    private val ignoredEntities = setOf(
        ArmorStand::class.java,
        ExperienceOrb::class.java,
        ItemEntity::class.java,
        ItemFrame::class.java,
        RemotePlayer::class.java,
        LocalPlayer::class.java,
    )

    @HandleEvent
    fun onDataWatcherUpdate(event: DataWatcherUpdatedEvent<Entity>) {
        for (updatedEntry in event.updatedEntries) {
            if (updatedEntry.accessor == Entity.DATA_CUSTOM_NAME) {
                EntityCustomNameUpdateEvent(event.entity, event.entity.customName.formattedTextCompatLessResets()).post()
            }

            if (updatedEntry.accessor == LivingEntity.DATA_HEALTH_ID) {
                val health = (updatedEntry.value as? Float)?.toInt() ?: continue

                val entity = EntityUtils.getEntityByID(event.entity.id) ?: continue
                if (entity.javaClass in ignoredEntities) continue

                if (event.entity is WitherBoss && health == 300 && event.entity.id < 0) continue
                if (event.entity is LivingEntity) {
                    EntityHealthUpdateEvent(event.entity, health.derpy()).post()
                }
            }
        }
    }
}
