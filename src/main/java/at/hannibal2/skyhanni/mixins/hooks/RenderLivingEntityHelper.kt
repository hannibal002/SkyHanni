package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtEvent
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RenderLivingEntityHelper {

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        entityColorMap.clear()
        entityNoHurTime.clear()
    }

    companion object {
        private val entityColorMap = mutableMapOf<EntityLivingBase, Int>()
        private val entityNoHurTime = mutableListOf<EntityLivingBase>()

        fun <T : EntityLivingBase> setEntityColor(entity: T, color: Int) {
            entityColorMap[entity] = color
        }

        fun <T : EntityLivingBase> setNoHurtTime(entity: T) {
            entityNoHurTime.add(entity)
        }

        fun <T : EntityLivingBase> setColorMultiplier(entity: T, ): Int {
            if (entityColorMap.containsKey(entity)) {
                return entityColorMap[entity]!!
            }

            //TODO remove event
            val event = RenderMobColoredEvent(entity, 0)
            event.postAndCatch()
            val color = event.color
            if (color != 0) {
                //TODO remove?
                entityColorMap[entity] = color
            }

            return color
        }

        fun <T : EntityLivingBase> changeHurtTime(entity: T): Int {
            if (entityNoHurTime.contains(entity)) {
                return 0
            }

            //TODO remove event
            val event = ResetEntityHurtEvent(entity, false)
            event.postAndCatch()
            val shouldReset = event.shouldReset

            if (shouldReset) {
                //TODO remove?
                entityNoHurTime.add(entity)
            }
            return if (shouldReset) 0 else entity.hurtTime
        }
    }
}