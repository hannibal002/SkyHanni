package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI.DungeonClass
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.exactPlayerEyeLocation

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

§c§lThe §6§lFLAME §c§ldragon is spawning!
§c§lThe §a§lAPEX §c§ldragon is spawning!
§c§lThe §c§lPOWER §c§ldragon is spawning!
§c§lThe §b§lICE §c§ldragon is spawning!
§c§lThe §5§lSOUL §c§ldragon is spawning!
 */

// TODO: Add spawn locations
enum class WitheredDragon(val spawnLocation: LorenzVec, val color: LorenzColor) { // bmh -> ogrbp <- at
    FLAME(LorenzVec(85.0, 14.0, 56.0), LorenzColor.GOLD),       // orange
    APEX(LorenzVec(27.0, 14.0, 94.0), LorenzColor.GREEN),       // green
    POWER(LorenzVec(27.0, 14.0, 59.0), LorenzColor.DARK_RED),   // red
    ICE(LorenzVec(84.0, 14.0, 94.0), LorenzColor.AQUA),         // blue
    SOUL(LorenzVec(56.0, 14.0, 125.0), LorenzColor.DARK_PURPLE) // purple
}

data class ArrowStackLocation(val witheredDragonType: WitheredDragon, val location: LorenzVec)

@SkyHanniModule
object ArrowStackWaypoints {

    private val spawnPattern = "§c§lThe §\\w§\\w(?<name>[A-Z]+) §c§ldragon is spawning!".toPattern()

    val locations = arrayOf(
        ArrowStackLocation(WitheredDragon.ICE, LorenzVec(48,5,110)),
        ArrowStackLocation(WitheredDragon.APEX, LorenzVec(25,6,119)),
        ArrowStackLocation(WitheredDragon.FLAME, LorenzVec(53,4,90)),
        ArrowStackLocation(WitheredDragon.POWER, LorenzVec(18,5,84)),
        ArrowStackLocation(WitheredDragon.POWER, LorenzVec(10,6,83)),
        ArrowStackLocation(WitheredDragon.SOUL, LorenzVec(31,5,97)),
        ArrowStackLocation(WitheredDragon.SOUL, LorenzVec(81,5,99))
    )

    val dragons = arrayListOf<WitheredDragon>()

    var closestLocation: ArrowStackLocation? = null
    var currentDragon: WitheredDragon? = null

    // TODO: Only bers and arch setting
    // TODO: Custom Prio settings
    // TODO: enable and disable feature

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!inDungeon()) return
        val matcher = spawnPattern.matcher(event.message)
        if (!matcher.find()) return
        val dragonName = matcher.group("name") ?: return
        dragons.add(WitheredDragon.entries.find { it.name == dragonName } ?: return)

        val dragsByPrio: List<WitheredDragon> = dragons.sortedBy { it.ordinal }
        val dragWithHighestPrio: WitheredDragon = if (DungeonAPI.playerClass == DungeonClass.BERSERK) {
            dragsByPrio.first()
        } else if (DungeonAPI.playerClass == DungeonClass.ARCHER) {
            dragsByPrio.last()
        } else {
            return
        }

        closestLocation = locations
            .filter { it.witheredDragonType == dragWithHighestPrio }
            .sortedBy { it.location.distance(Minecraft.getMinecraft().thePlayer.position.toLorenzVec()) }
            .last()
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!inDungeon()) return
        if (closestLocation != null) {
            event.drawColor(closestLocation!!.location, closestLocation!!.witheredDragonType.color, true, 0.4f)
            event.draw3DLine(
                event.exactPlayerEyeLocation(),
                closestLocation!!.location.add(0.5, 0.5, 0.5),
                closestLocation!!.witheredDragonType.color.toColor(),
                2,
                false
            )
        }
        if (currentDragon != null) {
            event.drawColor(currentDragon!!.spawnLocation, currentDragon!!.color, false, 0.4f)
            event.draw3DLine(
                event.exactPlayerEyeLocation(),
                currentDragon!!.spawnLocation.add(0.5, 0.5, 0.5),
                currentDragon!!.color.toColor(),
                2,
                false
            )
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!inDungeon() || closestLocation == null) return

        val playerPos = Minecraft.getMinecraft().thePlayer.position.toLorenzVec()
        val distance = playerPos.distance(closestLocation!!.location)
        if (distance < 1) {
            currentDragon = closestLocation!!.witheredDragonType
            closestLocation = null
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: LorenzWorldChangeEvent) {
        closestLocation = null
        currentDragon = null
        dragons.clear()
    }

    private fun inDungeon(): Boolean {
        if (!DungeonAPI.inDungeon()) return false
        if (!DungeonAPI.inBossRoom) return false
        if (!DungeonAPI.isOneOf("M7")) return false

        return true
    }
}
