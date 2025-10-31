package at.hannibal2.hanni.data.entity

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.HanniRenderEntityEvent
import at.hannibal2.hanni.events.entity.EntityOpacityActiveEvent
import at.hannibal2.hanni.events.entity.EntityOpacityEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.events.render.EntityRenderLayersEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.collection.CollectionUtils.containsKeys
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11

@HanniModule
object EntityOpacityManager {

    private var shouldHide: Boolean = false

    private var entities = emptyMap<EntityLivingBase, Int>()
    private var active = false

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        val event = EntityOpacityActiveEvent()
        event.post()
        active = event.isActive()
    }

    @HandleEvent(HanniTickEvent::class, onlyOnSkyblock = true)
    fun onTick() {
        if (!active) return
        val entities = mutableMapOf<EntityLivingBase, Int>()
        for (entity in EntityUtils.getEntitiesNextToPlayer<EntityLivingBase>(80.0)) {
            val event = EntityOpacityEvent(entity)
            event.post()
            event.opacity?.let {
                entities[entity] = it
            }
        }
        this.entities = entities
    }

    //#if MC > 1.21
    //$$ @JvmStatic
    //$$ fun getEntityOpacity(entity: LivingEntity): Int? {
    //$$     if (!active) return null
    //$$     if (!canChangeOpacity(entity)) return null
    //$$     return (opacity(entity) * 2.55).toInt()
    //$$ }
    //#endif

    private fun canChangeOpacity(entity: EntityLivingBase) = entities.containsKeys(entity) && opacity(entity) < 100

    private fun opacity(entity: EntityLivingBase): Int = entities[entity] ?: error("can not read opacity bc not in map")

    @HandleEvent
    fun onPreRender(event: HanniRenderEntityEvent.Pre<EntityLivingBase>) {
        if (!active) return
        val canChangeOpacity = canChangeOpacity(event.entity)
        shouldHide = canChangeOpacity
        if (!canChangeOpacity) return

        val opacity = opacity(event.entity)
        if (opacity <= 0) return event.cancel()

        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.color(1f, 1f, 1f, opacity / 100f)
    }

    @HandleEvent
    fun onPostRender(event: HanniRenderEntityEvent.Post<EntityLivingBase>) {
        if (!active) return
        if (!canChangeOpacity(event.entity)) return

        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.disableBlend()
    }

    @HandleEvent
    fun onRender(event: EntityRenderLayersEvent.Pre<EntityLivingBase>) {
        if (!active) return
        if (!canChangeOpacity(event.entity)) return
        if (!shouldHide) return
        event.cancel()
    }
}
