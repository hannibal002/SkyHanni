package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.events.LorenzChatEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/*
[
   {
      "x":18,
      "y":5,
      "z":84,
      "r":0,
      "g":1,
      "b":0,
      "options":{
         "name":"Red-1"
      }
   },
   {
      "x":10,
      "y":6,
      "z":83,
      "r":0,
      "g":1,
      "b":0,
      "options":{
         "name":"Red-2"
      }
   },
   {
      "x":53,
      "y":4,
      "z":90,
      "r":0,
      "g":1,
      "b":0,
      "options":{
         "name":"Orange"
      }
   },
   {
      "x":31,
      "y":5,
      "z":97,
      "r":0,
      "g":1,
      "b":0,
      "options":{
         "name":"Purple-1"
      }
   },
   {
      "x":81,
      "y":5,
      "z":99,
      "r":0,
      "g":1,
      "b":0,
      "options":{
         "name":"Purple-2"
      }
   },
   {
      "x":25,
      "y":6,
      "z":119,
      "r":0,
      "g":1,
      "b":0,
      "options":{
         "name":"Green"
      }
   },
   {
      "x":48,
      "y":5,
      "z":110,
      "r":0,
      "g":1,
      "b":0,
      "options":{
         "name":"Blue"
      }
   }
]
 */

// TODO: Add spawn locations
enum class WitheredDragon(val spawnLocation: LorenzVec, val color: LorenzColor) {
   ICE(LorenzVec(), LorenzColor.AQUA),
   SOUL(LorenzVec(), LorenzColor.LIGHT_PURPLE),
   FLAME(LorenzVec(), LorenzColor.GOLD),
   APEX(LorenzVec(), LorenzColor.GREEN),
   POWER(LorenzVec(), LorenzColor.DARK_RED)
}

data class ArrowStackLocation(val witheredDragonType: WitheredDragon, val location: LorenzVec)

object ArrowStackWaypoints {

   val locations = arrayOf(
      ArrowStackLocation(WitheredDragon.ICE, LorenzVec(48,5,110)),
      ArrowStackLocation(WitheredDragon.APEX, LorenzVec(25,6,119)),
      ArrowStackLocation(WitheredDragon.FLAME, LorenzVec(53,4,90)),
      ArrowStackLocation(WitheredDragon.POWER, LorenzVec(18,5,84)),
      ArrowStackLocation(WitheredDragon.POWER, LorenzVec(10,6,83)),
      ArrowStackLocation(WitheredDragon.SOUL, LorenzVec(31,5,97)),
      ArrowStackLocation(WitheredDragon.SOUL, LorenzVec(81,5,99))
   )

   var closestLocation: ArrowStackLocation = null

   // TODO: Detect class and the correct split to do
   // TODO: Detect closest waypoint from the list is spawning and matches
   // the current split
   // TODO: Add regex that matches the spawn message
   // TODO: Check correct floor etc.

   @SubscribeEvent
   fun onChat(event: LorenzChatEvent) {
      // TODO: enable and disable feature
      
   }

   @SubscribeEvent
   fun onRenderWorld(event: LorenzRenderWorldEvent) {
      // render waypoint and tracer to the correct location
      // when standing in the correct location tracer to the spawn location of the dragon for lining up
   }

}