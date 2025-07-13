package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIfKey
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import java.awt.Color

@SkyHanniModule
object RenderLivingEntityHelper {

    private val entityColorMap = mutableMapOf<EntityLivingBase, Int>()
    private val entityColorCondition = mutableMapOf<EntityLivingBase, () -> Boolean>()

    private val entityNoHurtTimeCondition = mutableMapOf<EntityLivingBase, () -> Boolean>()
    var areMobsHighlighted = false
    var renderingRealGlow = false
    var currentGlowEvent: RenderEntityOutlineEvent? = null

    fun isEntityInGlowEvent(entity: Entity): Int {
        return currentGlowEvent?.entitiesToOutline?.get(entity) ?: 0
    }

    @HandleEvent
    fun onWorldChange() {
        entityColorMap.clear()
        entityColorCondition.clear()

        entityNoHurtTimeCondition.clear()
    }

    @HandleEvent(SkyHanniTickEvent::class)
    fun onTick() {
        entityColorMap.removeIfKey { it.isDead }
        entityColorCondition.removeIfKey { it.isDead }
        entityNoHurtTimeCondition.removeIfKey { it.isDead }
    }

    fun check() {
        areMobsHighlighted = false
        for (entry in entityColorCondition) {
            if (entry.value.invoke()) {
                areMobsHighlighted = true
                return
            }
        }
        if (currentGlowEvent?.entitiesToOutline?.isNotEmpty() == true) areMobsHighlighted = true
    }

    fun <T : EntityLivingBase> removeEntityColor(entity: T) {
        entityColorMap.remove(entity)
        entityColorCondition.remove(entity)
    }

    fun <T : EntityLivingBase> setEntityColor(entity: T, color: Int, condition: () -> Boolean) {
        if (color == 0) return
        entityColorMap[entity] = color
        entityColorCondition[entity] = condition
    }

    fun <T : EntityLivingBase> setEntityColor(entity: T, color: Color, condition: () -> Boolean) {
        setEntityColor(entity, color.rgb, condition)
    }

    fun <T : EntityLivingBase> setNoHurtTime(entity: T, condition: () -> Boolean) {
        entityNoHurtTimeCondition[entity] = condition
    }

    fun <T : EntityLivingBase> setEntityColorWithNoHurtTime(entity: T, color: Int, condition: () -> Boolean) {
        setEntityColor(entity, color, condition)
        setNoHurtTime(entity, condition)
    }

    fun <T : EntityLivingBase> setEntityColorWithNoHurtTime(entity: T, color: Color, condition: () -> Boolean) {
        setEntityColorWithNoHurtTime(entity, color.rgb, condition)
    }

    fun <T : EntityLivingBase> removeNoHurtTime(entity: T) {
        entityNoHurtTimeCondition.remove(entity)
    }

    fun <T : EntityLivingBase> removeCustomRender(entity: T) {
        removeEntityColor(entity)
        removeNoHurtTime(entity)
    }

    @JvmStatic
    fun <T : EntityLivingBase> internalSetColorMultiplier(entity: T, default: Int): Int {
        if (!SkyHanniDebugsAndTests.globalRender) return default
        if (entityColorMap.containsKey(entity)) {
            val condition = entityColorCondition[entity] ?: return default
            if (condition.invoke()) {
                return entityColorMap[entity] ?: return default
            }
        }
        return default
    }

    @JvmStatic
    fun <T : EntityLivingBase> internalChangeHurtTime(entity: T): Int {
        if (!SkyHanniDebugsAndTests.globalRender) return entity.hurtTime
        run {
            val condition = entityNoHurtTimeCondition[entity] ?: return@run
            if (condition.invoke()) {
                return 0
            }
        }
        return entity.hurtTime
    }
}
