package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
//#if TODO
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
//#endif
import net.minecraft.entity.EntityLivingBase
import java.awt.Color

@SkyHanniModule
object RenderLivingEntityHelper {

    private val entityColorMap = mutableMapOf<EntityLivingBase, Int>()
    private val entityColorCondition = mutableMapOf<EntityLivingBase, () -> Boolean>()

    private val entityNoHurtTimeCondition = mutableMapOf<EntityLivingBase, () -> Boolean>()

    @HandleEvent
    fun onWorldChange() {
        entityColorMap.clear()
        entityColorCondition.clear()

        entityNoHurtTimeCondition.clear()
    }

    fun <T : EntityLivingBase> removeEntityColor(entity: T) {
        entityColorMap.remove(entity)
        entityColorCondition.remove(entity)
    }

    fun <T : EntityLivingBase> setEntityColor(entity: T, color: Int, condition: () -> Boolean) {
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
    fun <T : EntityLivingBase> internalSetColorMultiplier(entity: T): Int {
        //#if TODO
        if (!SkyHanniDebugsAndTests.globalRender) return 0
        //#endif
        if (entityColorMap.containsKey(entity)) {
            val condition = entityColorCondition[entity]!!
            if (condition.invoke()) {
                return entityColorMap[entity]!!
            }
        }
        return 0
    }

    @JvmStatic
    fun <T : EntityLivingBase> internalChangeHurtTime(entity: T): Int {
        //#if TODO
        if (!SkyHanniDebugsAndTests.globalRender) return entity.hurtTime
        //#endif
        run {
            val condition = entityNoHurtTimeCondition[entity] ?: return@run
            if (condition.invoke()) {
                return 0
            }
        }
        return entity.hurtTime
    }
}
