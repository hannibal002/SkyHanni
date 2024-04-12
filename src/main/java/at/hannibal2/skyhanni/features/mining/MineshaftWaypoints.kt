package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class MineshaftWaypoints {

    private val config get() = SkyHanniMod.feature.mining.mineshaftWaypoints

    private val waypoints: MutableList<Pair<LorenzVec, Color>> = mutableListOf()

    private val chatParser by RepoPattern.pattern(
        "mining.mineshaft.waypointmessage",
        "§9(?:P|Party).*: §r\\[SkyHanni] (?<type>Lapis|Umber|Tungsten|Vanguard) Corpse (?<x>\\d\\.\\d+) (?<y>\\d\\.\\d+) (?<z>\\d\\.\\d+)"
    )
//    private val armorParser by RepoPattern.pattern(
//        "mining.mineshaft.corpsearmor",
//        "(?<type>Lapis|Yog|Mineral|Vanguard) Chestplate"
//    )
    private val corpseCollect by RepoPattern.pattern(
        "mining.mineshaft.corpsecollect",
        "\\s*§r§b§lFROZEN CORPSE LOOT! |§cYou need to be holding an? .* Key §r§cto unlock this corpse!"
    )

    private val spawnVecOffset = LorenzVec(-4, 0, -2)

    @SubscribeEvent
    fun onJoinMineshaft(event: IslandChangeEvent) {
        if (event.newIsland != IslandType.MINESHAFT) return
        val coordinates = LocationUtils.playerLocation().add(spawnVecOffset)
        waypoints.add(Pair(coordinates, Color.DARK_GRAY))
    }

    @SubscribeEvent
    fun onWorldSwitch(event: LorenzWorldChangeEvent) {
        waypoints.clear()
    }

    @SubscribeEvent
    fun onRender(event: LorenzRenderWorldEvent) {
        if (!config.enabled) return
        if (!IslandType.MINESHAFT.isInIsland()) return
        waypoints.forEach { (location, color) ->
            if (!config.showEntrance && color == Color.DARK_GRAY) return@forEach
            event.drawWaypointFilled(location, color, seeThroughBlocks = true, beacon = true)
            location.toCleanString()
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!IslandType.MINESHAFT.isInIsland()) return
        if (corpseCollect.matches(event.message)) {
            findCorpse()
            return
        }
        if (!config.receiveChat) return

        val matcher = chatParser.matcher(event.message)
        if (!matcher.matches()) return
        val x = matcher.group("x").toDoubleOrNull() ?: return
        val y = matcher.group("y").toDoubleOrNull() ?: return
        val z = matcher.group("z").toDoubleOrNull() ?: return
        val location = LorenzVec(x, y, z)
        if (isVecInList(location)) return
        val color = getArmorColor(matcher.group("type"))
        if (color == Color.WHITE) return
        waypoints.add(Pair(location, color))
    }

    private fun findCorpse() {
        for (entity in EntityUtils.getAllEntities().filter { it is EntityArmorStand
                && it.position.toLorenzVec().distanceToPlayer() < 6.0
                && !it.isInvisible }) {
            val message = "[SkyHanni] TODO Corpse ${entity.position.toLorenzVec().toCleanString()}"
            ChatUtils.debug("found corpse at ${entity.position.toLorenzVec().toCleanString()}")
            if (config.sendChat) ChatUtils.sendCommandToServer("pc $message")
        }
    }

    private fun getArmorColor(type: String?): Color {
        if (type == null) return Color.WHITE
        return when (type) {
            "Lapis", "Lapis Armor Chestplate" -> Color.BLUE
            "Umber", "Yog Chestplate" -> Color.ORANGE
            "Tungsten", "Mineral Chestplate" -> Color.LIGHT_GRAY
            "Vanguard", "Vanguard Chestplate" -> Color.CYAN
            else -> Color.WHITE
        }
    }

    private fun isVecInList(location: LorenzVec): Boolean {
        for ((vec, _) in waypoints) {
            if (vec == location) return true
        }
        return false
    }

    @SubscribeEvent
    fun onDebug(event: DebugDataCollectEvent){
        event.title("mineshaft waypoints")
        if (!IslandType.MINESHAFT.isInIsland()) {
            event.addIrrelevant("not in mineshaft")
            return
        }
        waypoints.forEach { (location, color) ->
            event.addData("$location, $color")
        }
    }
}