package at.hannibal2.skyhanni.data.entity

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.entity.EntityTransparencyActiveEvent
import at.hannibal2.skyhanni.events.entity.EntityTransparencyTickEvent
import at.hannibal2.skyhanni.events.render.EntityRenderLayersEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.associateNotNull
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.containsKeys
import at.hannibal2.skyhanni.utils.collection.TimeLimitedValue
import net.minecraft.world.entity.LivingEntity
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object EntityTransparencyManager {
    private var entities = emptyMap<LivingEntity, Int>()

    private val activeValue = TimeLimitedValue(1.seconds) {
        EntityTransparencyActiveEvent().apply { post() }.isActive()
    }
    private val active get() = activeValue.get() ?: false

    @HandleEvent(onlyOnSkyblock = true)
    private fun onTick() {
        if (!active) return
        this.entities = EntityUtils.getEntitiesNearby<LivingEntity>(80.0).associateNotNull status@{ entity ->
            val event = EntityTransparencyTickEvent(entity).apply { post() }
            val newTransparency = event.newTransparency ?: return@status null
            entity to newTransparency
        }
    }

    @JvmStatic
    fun getEntityTransparency(entity: LivingEntity): Int? {
        if (!active || !canChangeTransparency(entity)) return null
        return (getTransparency(entity) * 2.55).toInt()
    }

    private fun canChangeTransparency(entity: LivingEntity) =
        entities.containsKeys(entity) && getTransparency(entity) < 100

    private fun getTransparency(entity: LivingEntity): Int = entities[entity]
        ?: error("Cannot read opacity because entity is not in the map")

    private fun processEntityTransparency(entity: LivingEntity, cancel: () -> Unit) {
        if (!active || !canChangeTransparency(entity)) return
        if (getTransparency(entity) <= 0) cancel()
    }

    @HandleEvent
    private fun onCheckRender(event: CheckRenderEntityEvent<LivingEntity>) =
        processEntityTransparency(event.entity, event::cancel)

    @HandleEvent
    private fun onRender(event: EntityRenderLayersEvent.Pre<LivingEntity>) =
        processEntityTransparency(event.entity, event::cancel)
}
