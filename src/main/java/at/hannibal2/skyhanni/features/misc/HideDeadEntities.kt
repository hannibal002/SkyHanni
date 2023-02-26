//package at.hannibal2.skyhanni.features.misc
//
//import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
//import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
//import at.hannibal2.skyhanni.utils.LorenzUtils
//import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
//
//
//// Skytils feature does work already.
//
//class HideDeadEntities {
//    private val hiddenEntities = mutableListOf<Int>()
//
//    @SubscribeEvent
//    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
//        val entityId = event.entity.entityId
//
//        if (event.health <= 0) {
//            if (!hiddenEntities.contains(entityId)) {
//                hiddenEntities.add(entityId)
//            }
//        } else {
//            if (hiddenEntities.contains(entityId)) {
//                hiddenEntities.remove(entityId)
//                LorenzUtils.debug("respawned: ${event.entity.name}")
//            }
//        }
//    }
//
//    @SubscribeEvent
//    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
//        if (!LorenzUtils.inSkyBlock) return
//
////        if (hiddenEntities.contains(event.entity.entityId)) {
////            event.isCanceled = true
////        }
//    }
//}