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
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class MineshaftWaypoints {

    private val config get() = SkyHanniMod.feature.mining.mineshaftWaypoints

    private val chatParser by RepoPattern.pattern(
        "mining.mineshaft.waypointmessage",
        "§9(?:P|Party).*: §r\\[SkyHanni] (?<type>Lapis|Umber|Tungsten|Vanguard) Corpse (?<x>-?\\d+\\.\\d+) (?<y>\\d+\\.\\d+) (?<z>-?\\d+\\.\\d+)"
    )
    private val armorParser by RepoPattern.pattern(
        "mining.mineshaft.corpsearmor",
        "§.(?<type>Lapis|Yog|Mineral|Vanguard)(?: Armor)? Chestplate"
    )
    private val corpseCollect by RepoPattern.pattern(
        "mining.mineshaft.corpsecollect",
        "\\s*§r§b§lFROZEN CORPSE LOOT! |§cYou need to be holding an? .* Key §r§cto unlock this corpse!|§cYou've already looted this corpse!"
    )

    private enum class MineshaftWaypointType(val color: Color, val waypointText: String, val armorName: String) {
        LAPIS(Color.BLUE, "§9Lapis", "§aLapis Armor Chestplate"),
        UMBER(Color.ORANGE, "§cUmber", "§5Yog Chestplate"),
        TUNGSTEN(Color.LIGHT_GRAY, "§7Tungsten", "§5Mineral Chestplate"),
        VANGUARD(Color.CYAN, "§bVanguard", "§6Vanguard Chestplate"),
        ENTRANCE(Color.GREEN, "§aEntrance", ""),
        UNKNOWN(Color.WHITE, "report this please", "");
    }

    private val waypoints: MutableList<Triple<LorenzVec, MineshaftWaypointType, Boolean>> = mutableListOf()

    @SubscribeEvent
    fun onJoinMineshaft(event: IslandChangeEvent) {
        if (event.newIsland != IslandType.MINESHAFT) return
        val coordinates = LocationUtils.playerLocation().round(0).add(y = -1)
        waypoints.add(Triple(coordinates, MineshaftWaypointType.ENTRANCE, false))
    }

    @SubscribeEvent
    fun onWorldSwitch(event: LorenzWorldChangeEvent) {
        waypoints.clear()
    }

    @SubscribeEvent
    fun onRender(event: LorenzRenderWorldEvent) {
        if (!config.enabled) return
        if (!IslandType.MINESHAFT.isInIsland()) return
        waypoints.forEach { (location, type, isCollected) ->
            if (!config.showEntrance && type == MineshaftWaypointType.ENTRANCE) return@forEach
            if (config.delete && isCollected) return@forEach
            if (config.drawText) {
                val text = type.waypointText
                event.drawDynamicText(location.add(y = 1), text, 1.0)
            }
            event.drawWaypointFilled(location, type.color, seeThroughBlocks = true)
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
        if (config.hideMessage) event.blockedReason = "corpse_sharing"
        val x = matcher.group("x").toDoubleOrNull() ?: return
        val y = matcher.group("y").toDoubleOrNull() ?: return
        val z = matcher.group("z").toDoubleOrNull() ?: return
        val location = LorenzVec(x, y, z)
        if (isVecInList(location)) {
            ChatUtils.debug("duplicate location")
            return
        }
        val type = nameToType(matcher.group("type"))
        if (type == MineshaftWaypointType.UNKNOWN) return
        waypoints.add(Triple(location, type, false))
    }

    private fun findCorpse() {
        for (entity in EntityUtils.getAllEntities().filter {
            (it is EntityArmorStand
                    && it.position.toLorenzVec().distanceToPlayer() < 6.0
                    && !it.isInvisible)
        }) {
            entity as EntityArmorStand
            val location = entity.position.toLorenzVec()
            if (isVecInList(location) && config.delete) {
                waypoints.forEachIndexed { index, triple ->
                    if (triple.first == location) {
                        val newTriple = Triple(triple.first, triple.second, true)
                        waypoints[index] = newTriple
                    }
                }
                return
            }
            val armor = entity.getCurrentArmor(2) ?: return
            val armorName = armor.displayName
            val matcher = armorParser.matcher(armorName)
            if (!matcher.matches()) {
                ChatUtils.debug("armor doesn't match '${armorName}'")
                return
            }
            val type = nameToType(matcher.group("type").removeColor())
            val position = entity.position.toLorenzVec()
            val message = "[SkyHanni] ${type.waypointText.removeColor()} Corpse ${position.toCleanString()}"
            waypoints.add(Triple(position, type, true))
            if (config.sendChat && !isVecInList(position)) ChatUtils.sendCommandToServer("pc $message")
            ChatUtils.debug("found corpse at $position}")
        }
    }

    private fun nameToType(type: String): MineshaftWaypointType {
        return when (type) {
            "Lapis" -> MineshaftWaypointType.LAPIS
            "Umber", "Yog" -> MineshaftWaypointType.UMBER
            "Tungsten", "Mineral" -> MineshaftWaypointType.TUNGSTEN
            "Vanguard" -> MineshaftWaypointType.VANGUARD
            "Entrance" -> MineshaftWaypointType.ENTRANCE
            else -> {
                ChatUtils.debug("weird string! '$type'")
                MineshaftWaypointType.UNKNOWN
            }
        }
    }

    private fun isVecInList(location: LorenzVec): Boolean {
        for ((vec, _) in waypoints) {
            if (vec == location) return true
        }
        return false
    }

    @SubscribeEvent
    fun onDebug(event: DebugDataCollectEvent) {
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