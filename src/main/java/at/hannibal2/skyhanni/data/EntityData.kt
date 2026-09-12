package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ElectionApi.derpy
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.entity.EntityAttributeUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityDisplayNameEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthDisplayEvent
import at.hannibal2.skyhanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.skyhanni.events.entity.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.player.RemotePlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object EntityData {

    private val nametagCache = TimeLimitedCache<UUID, Component>(50.milliseconds)
    private val healthDisplayCache = TimeLimitedCache<Component, Component>(50.milliseconds)
    private val lastVisibilityCheck = TimeLimitedCache<Int, Boolean>(200.milliseconds)

    val ignoredEntities = setOf(
        ArmorStand::class.java,
        ExperienceOrb::class.java,
        ItemEntity::class.java,
        ItemFrame::class.java,
        RemotePlayer::class.java,
        LocalPlayer::class.java,
    )

    @HandleEvent
    private fun onEntityAttributeUpdate(event: EntityAttributeUpdateEvent<LivingEntity>) {
        val entity = event.entity
        if (entity.javaClass in ignoredEntities) return
        val attribute = event.attribute
        if (attribute.`is`(Attributes.MAX_HEALTH)) {
            val maxHealth = entity.baseMaxHealth
            EntityMaxHealthUpdateEvent(entity, maxHealth.derpy()).post()
        }
    }

    @JvmStatic
    fun getDisplayName(entity: Entity, oldValue: Component): Component {
        return postRenderNametag(entity, oldValue)
    }

    @JvmStatic
    fun despawnEntity(entity: Entity) {
        EntityLeaveWorldEvent(entity).post()
    }

    private fun postRenderNametag(entity: Entity, chatComponent: Component) = nametagCache.getOrPut(entity.uuid) {
        val event = EntityDisplayNameEvent(entity, chatComponent)
        event.post()
        event.chatComponent
    }

    @JvmStatic
    fun getHealthDisplay(text: Component) = healthDisplayCache.getOrPut(text) {
        val event = EntityHealthDisplayEvent(text)
        event.post()
        event.text
    }

    @JvmStatic
    fun shouldRender(entity: Entity, camX: Double, camY: Double, camZ: Double): Boolean {
        if (GlobalRender.renderDisabled) return true
        lastVisibilityCheck[entity.id]?.let { result ->
            return result
        }
        val result = !CheckRenderEntityEvent(entity, camX, camY, camZ).post().isCancelled
        lastVisibilityCheck[entity.id] = result
        return result
    }

    @JvmStatic
    fun onDisplayRenderStateUpdate(display: Display) {
        lastVisibilityCheck.remove(display.id)
    }
}
