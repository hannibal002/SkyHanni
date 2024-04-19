package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.StringUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

// TODO: Implement a key bind to send the location of a nearby corpse, considering the range of 5 blocks or possibly less.
// TODO: Maybe implement automatic warp-in for chosen players if the user is not in a party.
object CorpseLOS {
    private val config get() = SkyHanniMod.feature.mining.corpseLOS

    /**
     * I have no clue what to do for the regex anymore, I'm slowly losing it
     * REGEX-TEST: §9Party §8> §b[MVP§0+]§b nobaboy§f: §rx: -164, y: 8, z: -154 | (Lapis Corpse)
     * REGEX-TEST: Guild > §r§6♣ §b[MVP§0+§b] nobaboy§f: x: 141, y: 14, z: -131
     * REGEX-TEST: §8[§r§6407§r§8] §r§6♣ §b[MVP§0+§b] nobaboy§f: x: -9, y: 135, z: 20 | (Tungsten Corpse)
     */
    private val corpseCoordsWaypoint by RepoPattern.pattern(
        "corpse.waypoints",
        "^(?:.+?[^x:y0-9] )?(?<playerName>.+)§f: (?:§r)?x: (?<x>-?\\d+),? y: (?<y>-?\\d+),? z: (?<z>-?\\d+)(?:.+)?"
    )

    private val detectedArmorStands: MutableList<Corpse> = mutableListOf()
    private val parsedLocations: MutableList<LorenzVec> = mutableListOf()

    // Could move this into its own class to reduce clutter
    enum class CorpseType(val helmetName: String, val color: LorenzColor) {
        LAPIS("Lapis Armor Helmet", LorenzColor.DARK_BLUE),
        UMBER("Yog Helmet", LorenzColor.GOLD),
        TUNGSTEN("Mineral Helmet", LorenzColor.GRAY),
        VANGUARD("Vanguard Helmet", LorenzColor.BLUE)
    }

    data class Corpse(val type: CorpseType, val coords: LorenzVec, var shared: Boolean = false)

    private fun findCorpse() {
        Minecraft.getMinecraft().theWorld ?: return
        EntityUtils.getAllEntities().filterIsInstance<EntityArmorStand>()
            .filterNot { corpse ->
                detectedArmorStands.any { it.coords.distance(corpse.getLorenzVec()) <= 3 }
            }
            .filter { entity ->
                entity.showArms && entity.hasNoBasePlate() && !entity.isInvisible
            }
            .forEach { entity ->
                val helmetDisplayName = StringUtils.stripControlCodes(entity.getCurrentArmor(3).displayName)
                val corpseType = CorpseType.entries.firstOrNull { it.helmetName == helmetDisplayName }
                corpseType?.let { type ->
                    for (offset in -1..3) {
                        val canSee = entity.getLorenzVec().add(y = offset).canBeSeen()
                        if (canSee) {
                            detectedArmorStands.add(Corpse(type = type, coords = entity.getLorenzVec(), shared = false))
                            ChatUtils.chat("Located a ${type.name.firstLetterUppercase()} Corpse and marked its location with a waypoint.")
                            break
                        }
                    }
                }
            }
    }

    private fun shareCoords() {
        detectedArmorStands.filterNot { it.shared }
            .filterNot { corpse ->
                parsedLocations.any { corpse.coords.distance(it) <= 5 }
            }
            .filter { it.coords.distanceToPlayer() <= 5 }
            .forEach { corpse ->
                val location = corpse.coords
                val x = location.x.toInt()
                val y = location.y.toInt()
                val z = location.z.toInt()
                val type = corpse.type.name.firstLetterUppercase()

                ChatUtils.sendCommandToServer("pc x: $x, y: $y, z: $z | ($type Corpse)")
                corpse.shared = true
            }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        detectedArmorStands.clear()
        parsedLocations.clear()
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        findCorpse()

        if (!config.autoSendLocation) return
        if (detectedArmorStands.isEmpty()) return
        if (PartyAPI.partyMembers.isEmpty()) return
        shareCoords()
    }

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (detectedArmorStands.isEmpty()) return

        detectedArmorStands.forEach {
                val location = it.coords.add(y = 1)
                event.drawWaypointFilled(location, it.type.color.toColor())
                event.drawDynamicText(location, "§e${it.type.name.firstLetterUppercase()} Corpse", 1.0)
            }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val message = event.message

        corpseCoordsWaypoint.matchMatcher(message) {
            val name = group("playerName").cleanPlayerName()
            if (Minecraft.getMinecraft().thePlayer.name in name) return

            val x = group("x").trim().toInt()
            val y = group("y").trim().toInt()
            val z = group("z").trim().toInt()
            val location = LorenzVec(x, y, z)

            // Return if someone had already sent a location nearby
            if (parsedLocations.any { it.distance(location) <= 5 }) return
            parsedLocations.add(location)
        }
    }

    fun isEnabled() = IslandType.MINESHAFT.isInIsland() && config.enabled
}
