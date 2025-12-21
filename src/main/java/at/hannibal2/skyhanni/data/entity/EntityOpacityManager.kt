package at.hannibal2.skyhanni.data.entity

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent
import at.hannibal2.skyhanni.events.entity.EntityOpacityActiveEvent
import at.hannibal2.skyhanni.events.entity.EntityOpacityEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.render.EntityRenderLayersEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.containsKeys
import net.minecraft.world.entity.LivingEntity

@SkyHanniModule
object EntityOpacityManager {

    private var shouldHide: Boolean = false

    private var entities = emptyMap<LivingEntity, Int>()
    private var active = false

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        val event = EntityOpacityActiveEvent()
        event.post()
        active = event.isActive()
    }

    @HandleEvent(SkyHanniTickEvent::class, onlyOnSkyblock = true)
    fun onTick() {
        if (!active) return
        val entities = mutableMapOf<LivingEntity, Int>()
        for (entity in EntityUtils.getEntitiesNextToPlayer<LivingEntity>(80.0)) {
            val event = EntityOpacityEvent(entity)
            event.post()
            event.opacity?.let {
                entities[entity] = it
            }
        }
        this.entities = entities
    }

    @JvmStatic
    fun getEntityOpacity(entity: LivingEntity): Int? {
        if (!active) return null
        if (!canChangeOpacity(entity)) return null
        return (opacity(entity) * 2.55).toInt()
    }

    private fun canChangeOpacity(entity: LivingEntity) = entities.containsKeys(entity) && opacity(entity) < 100

    private fun opacity(entity: LivingEntity): Int = entities[entity] ?: error("can not read opacity bc not in map")

    @HandleEvent
    fun onPreRender(event: SkyHanniRenderEntityEvent.Pre<LivingEntity>) {
        if (!active) return
        val canChangeOpacity = canChangeOpacity(event.entity)
        shouldHide = canChangeOpacity
        if (!canChangeOpacity) return

        val opacity = opacity(event.entity)
        if (opacity <= 0) return event.cancel()
    }

    @HandleEvent
    fun onRender(event: EntityRenderLayersEvent.Pre<LivingEntity>) {
        if (!active) return
        if (!canChangeOpacity(event.entity)) return
        if (!shouldHide) return
        event.cancel()
    }
}
