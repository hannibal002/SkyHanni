package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ElectionApi.derpy
import at.hannibal2.skyhanni.events.DataWatcherUpdatedEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityRemovedEvent
import at.hannibal2.skyhanni.events.entity.EntityTextRemovedEvent
import at.hannibal2.skyhanni.events.entity.EntityTextUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Display
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
                EntityCustomNameUpdateEvent(event.entity, event.entity.customName).post()
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

    @HandleEvent(onlyOnSkyblock = true)
    fun onArmorStandNameUpdate(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        EntityTextUpdateEvent(event.entity, event.newName).post()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTextDisplayUpdate(event: EntityCustomNameUpdateEvent<Display.TextDisplay>) {
        val entity = event.entity
        EntityTextUpdateEvent(entity, entity.text).post()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onArmorStandRemoved(event: EntityRemovedEvent<ArmorStand>) {
        EntityTextRemovedEvent(event.entity).post()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTextDisplayRemoved(event: EntityRemovedEvent<Display.TextDisplay>) {
        EntityTextRemovedEvent(event.entity).post()
    }
}
