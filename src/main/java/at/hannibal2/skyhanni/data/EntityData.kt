package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ElectionApi.derpy
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.entity.EntityDisplayNameEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthDisplayEvent
import at.hannibal2.skyhanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.skyhanni.events.entity.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object EntityData {

    private val maxHealthMap = mutableMapOf<Int, Int>()
    private val nametagCache = TimeLimitedCache<Entity, Component>(50.milliseconds)
    private val healthDisplayCache = TimeLimitedCache<Component, Component>(50.milliseconds)
    private val lastVisibilityCheck = TimeLimitedCache<Int, Boolean>(200.milliseconds)

    // TODO replace with packet detection
    @OptIn(AllEntitiesGetter::class)
    @HandleEvent
    fun onTick() {
        for (entity in EntityUtils.getEntities<LivingEntity>()) { // this completely ignores the ignored entities list?
            val maxHealth = entity.baseMaxHealth
            val id = entity.id
            val oldMaxHealth = maxHealthMap.getOrDefault(id, -1)
            if (oldMaxHealth != maxHealth) {
                maxHealthMap[id] = maxHealth
                EntityMaxHealthUpdateEvent(entity, maxHealth.derpy()).post()
            }
        }
    }

    @HandleEvent
    fun onEntityLeaveWorld(event: EntityLeaveWorldEvent<LivingEntity>) {
        maxHealthMap -= event.entity.id
    }

    @HandleEvent
    fun onWorldChange() {
        maxHealthMap.clear()
    }

    @JvmStatic
    fun getDisplayName(entity: Entity, oldValue: Component): Component {
        return postRenderNametag(entity, oldValue)
    }

    @JvmStatic
    fun despawnEntity(entity: Entity) {
        EntityLeaveWorldEvent(entity).post()
    }

    private fun postRenderNametag(entity: Entity, chatComponent: Component) = nametagCache.getOrPut(entity) {
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
    fun onRenderCheck(entity: Entity, camX: Double, camY: Double, camZ: Double): Boolean {
        if (GlobalRender.renderDisabled) return true
        lastVisibilityCheck[entity.id]?.let { result ->
            return result
        }
        val result = CheckRenderEntityEvent(entity, camX, camY, camZ).post()
        lastVisibilityCheck[entity.id] = result
        return result
    }
}
