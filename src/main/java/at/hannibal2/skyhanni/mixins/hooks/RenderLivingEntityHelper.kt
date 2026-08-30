package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.GlobalRender
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils.hasVisibleEquipment
import net.minecraft.util.ARGB
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import java.awt.Color

//? if >= 26.2 {
import net.azureaaron.renderchest.api.CustomGlowCallback
import net.azureaaron.renderchest.api.GlowConstants
//?}

@SkyHanniModule
object RenderLivingEntityHelper {
    private data class EntityGlowData(val rgb: Int, val condition: () -> Boolean)

    private val entityGlowMap = mutableMapOf<Int, EntityGlowData>()
    private var currentGlowEvent: RenderEntityOutlineEvent? = null

    //? if >= 26.2 {
    init {
        CustomGlowCallback.EVENT.register { entity, _ ->
            ARGB.opaque(getEntityGlowColor(entity)) ?: GlowConstants.NO_GLOW
        }
    }
    //?} else {
    /*@JvmStatic
    var isUsingCustomGlow = false
        private set
    *///?}

    @JvmStatic
    fun postNoXrayOutlineEvent() {
        //? if < 26.2 {
        /*isUsingCustomGlow = entityGlowMap.values.any { it.condition() } ||
            currentGlowEvent?.entitiesToOutline.orEmpty().isNotEmpty()
        *///?}
        val event = RenderEntityOutlineEvent()
        currentGlowEvent = event
        event.post()
    }

    @JvmStatic
    fun getEntityGlowColor(entity: Entity): Int? {
        if (GlobalRender.renderDisabled) return null
        if (entity is LivingEntity) {
            if (entity.isInvisible && !entity.hasVisibleEquipment()) return null
            getLivingEntityGlowColor(entity)?.let { return it }
        }
        return getEntityGlowEventColor(entity)
    }

    private fun getEntityGlowEventColor(entity: Entity): Int? =
        currentGlowEvent?.entitiesToOutline?.get(entity)

    private fun getLivingEntityGlowColor(entity: LivingEntity): Int? {
        val entityGlowData = entityGlowMap[entity.id] ?: return null
        if (!entityGlowData.condition()) return null
        return entityGlowData.rgb
    }

    @HandleEvent
    private fun onWorldChange() {
        entityGlowMap.clear()
    }

    @HandleEvent
    private fun onEntityLeaveWorld(event: EntityLeaveWorldEvent<LivingEntity>) {
        entityGlowMap.remove(event.entity.id)
    }

    fun <T : LivingEntity> removeEntityColor(entity: T) {
        val entityId = entity.id
        DelayedRun.runOrNextTick {
            entityGlowMap.remove(entityId)
        }
    }

    fun <T : LivingEntity> setEntityColor(entity: T, color: Color, condition: () -> Boolean) {
        val rgb = color.rgb.takeUnless { it == 0 } ?: return
        val entityId = entity.id
        DelayedRun.runOrNextTick {
            entityGlowMap[entityId] = EntityGlowData(rgb, condition)
        }
    }
}
