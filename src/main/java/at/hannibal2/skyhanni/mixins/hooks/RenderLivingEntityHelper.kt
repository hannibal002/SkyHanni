package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtEvent
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RenderLivingEntityHelper {

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        entityColorMap.clear()
        entityColorCondition.clear()

        entityNoHurTime.clear()
        entityNoHurTimeCondition.clear()
    }

    companion object {
        private val entityColorMap = mutableMapOf<EntityLivingBase, Int>()
        private val entityColorCondition = mutableMapOf<EntityLivingBase, () -> Boolean>()

        private val entityNoHurTime = mutableListOf<EntityLivingBase>()
        private val entityNoHurTimeCondition = mutableMapOf<EntityLivingBase, () -> Boolean>()

        fun <T : EntityLivingBase> removeEntityColor(entity: T) {
            entityColorMap.remove(entity)
            entityColorCondition.remove(entity)
        }

        fun <T : EntityLivingBase> setEntityColor(entity: T, color: Int, condition: () -> Boolean) {
            entityColorMap[entity] = color
            entityColorCondition[entity] = condition
        }

        fun <T : EntityLivingBase> setNoHurtTime(entity: T, condition: () -> Boolean) {
            entityNoHurTime.add(entity)
            entityNoHurTimeCondition[entity] = condition
        }

        fun <T : EntityLivingBase> setColorMultiplier(entity: T): Int {
            if (!SkyHanniDebugsAndTests.globalRender) return 0
            if (entityColorMap.containsKey(entity)) {
                val condition = entityColorCondition[entity]!!
                if (condition.invoke()) {
                    return entityColorMap[entity]!!
                }
            }

            //TODO remove event
            if (!SkyHanniDebugsAndTests.globalRender) return 0
            val event = RenderMobColoredEvent(entity, 0)
            event.postAndCatch()
            return event.color
        }

        fun <T : EntityLivingBase> changeHurtTime(entity: T): Int {
            if (!SkyHanniDebugsAndTests.globalRender) return 0
            if (entityNoHurTime.contains(entity)) {
                val condition = entityNoHurTimeCondition[entity]!!
                if (condition.invoke()) {
                    return 0
                }
            }

            //TODO remove event
            val event = ResetEntityHurtEvent(entity, false)
            event.postAndCatch()
            return if (event.shouldReset) 0 else entity.hurtTime
        }
    }
}