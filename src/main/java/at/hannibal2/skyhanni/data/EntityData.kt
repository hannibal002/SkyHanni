package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.ElectionApi.derpy
import at.hannibal2.hanni.events.CheckRenderEntityEvent
import at.hannibal2.hanni.events.entity.EntityDisplayNameEvent
import at.hannibal2.hanni.events.entity.EntityHealthDisplayEvent
import at.hannibal2.hanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.hanni.events.entity.EntityMaxHealthUpdateEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.hanni.utils.collection.TimeLimitedCache
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.ChatComponentText
import kotlin.time.Duration.Companion.milliseconds

@HanniModule
object EntityData {

    private val maxHealthMap = mutableMapOf<Int, Int>()
    private val nametagCache = TimeLimitedCache<Entity, ChatComponentText>(50.milliseconds)
    private val healthDisplayCache = TimeLimitedCache<String, String>(50.milliseconds)
    private val lastVisibilityCheck = TimeLimitedCache<Int, Boolean>(200.milliseconds)

    // TODO replace with packet detection
    @HandleEvent
    fun onTick() {
        for (entity in EntityUtils.getEntities<EntityLivingBase>()) { // this completely ignores the ignored entities list?
            val maxHealth = entity.baseMaxHealth
            val id = entity.entityId
            val oldMaxHealth = maxHealthMap.getOrDefault(id, -1)
            if (oldMaxHealth != maxHealth) {
                maxHealthMap[id] = maxHealth
                EntityMaxHealthUpdateEvent(entity, maxHealth.derpy()).post()
            }
        }
    }

    @HandleEvent
    fun onEntityLeaveWorld(event: EntityLeaveWorldEvent<EntityLivingBase>) {
        maxHealthMap -= event.entity.entityId
    }

    @HandleEvent
    fun onWorldChange() {
        maxHealthMap.clear()
    }

    @JvmStatic
    fun getDisplayName(entity: Entity, oldValue: ChatComponentText): ChatComponentText {
        return postRenderNametag(entity, oldValue)
    }

    @JvmStatic
    fun despawnEntity(entity: Entity) {
        EntityLeaveWorldEvent(entity).post()
    }

    private fun postRenderNametag(entity: Entity, chatComponent: ChatComponentText) = nametagCache.getOrPut(entity) {
        val event = EntityDisplayNameEvent(entity, chatComponent)
        event.post()
        event.chatComponent
    }

    @JvmStatic
    fun getHealthDisplay(text: String) = healthDisplayCache.getOrPut(text) {
        val event = EntityHealthDisplayEvent(text)
        event.post()
        event.text
    }

    @JvmStatic
    fun onRenderCheck(entity: Entity, camX: Double, camY: Double, camZ: Double): Boolean {
        if (GlobalRender.renderDisabled) return true
        lastVisibilityCheck[entity.entityId]?.let { result ->
            return result
        }
        val result = CheckRenderEntityEvent(entity, camX, camY, camZ).post()
        lastVisibilityCheck[entity.entityId] = result
        return result
    }
}
