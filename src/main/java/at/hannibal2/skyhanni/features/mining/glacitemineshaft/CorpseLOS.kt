package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
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
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.StringUtils
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

object CorpseLOS {
    private val config get() = SkyHanniMod.feature.mining.corpseLOS

    // TODO make a group for both party and all chat coords messages
    private val partyCorpseCoords =
        Pattern.compile("(?<party>§9Party §8> )?(?<playerName>.+)§f: §rx: (?<x>[^ ]+), y: (?<y>[^ ]+), z: (?<z>[^ ]+)")

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
        for (entity in EntityUtils.getAllEntities()) {
            if (entity !is EntityArmorStand) continue
            if (detectedArmorStands.any { it.coords == entity.getLorenzVec()}) continue
            if (entity.showArms && entity.hasNoBasePlate() && !entity.isInvisible) {
                for (offset in -1..3) { // Offset for search, feels a bit too borderline but best I can do to deal with glass/ice
                    val canSee = entity.getLorenzVec().add(y = offset).canBeSeen()
                    if (canSee) {
                        val helmetDisplayName = StringUtils.stripControlCodes(entity.getCurrentArmor(3).displayName)
                        val corpseType = CorpseType.entries.firstOrNull { it.helmetName == helmetDisplayName }
                        corpseType?.let {
                            sendCorpseAlert(it)
                            detectedArmorStands.add(Corpse(type = it, coords = entity.getLorenzVec(), shared = false))
                        }
                        break // Break the inner loop if the corpse is detected from any offset
                    }
                }
            }
        }
    }

    private fun sendCorpseAlert(type: CorpseType) {
        val message = "Discovered the ${type.name.firstLetterUppercase()} Corpse and marked its location with a waypoint."
        ChatUtils.chat(message)
    }

    private fun shareCoords() {
        for (corpse in detectedArmorStands) {
            if (parsedLocations.any { corpse.coords.distance(it) <= 5}) corpse.shared = true
            if (corpse.shared) continue

            val distToPlayer = corpse.coords.distanceToPlayer()
            if (distToPlayer > 4) continue

            val corpseLocation = corpse.coords
            val x = corpseLocation.x.toInt()
            val y = corpseLocation.y.toInt()
            val z = corpseLocation.z.toInt()
            val type = corpse.type.name.firstLetterUppercase()

            ChatUtils.sendCommandToServer("pc x: $x, y: $y, z: $z | ($type Corpse)")
            corpse.shared = true
        }
    }

    @SubscribeEvent
    fun onWorldUnload(ignored: WorldEvent.Unload?) {
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

        for (corpse in detectedArmorStands) {
            val location = corpse.coords.add(y = 1)
            event.drawWaypointFilled(location, corpse.type.color.toColor())
            event.drawDynamicText(location, "§e${corpse.type.name.firstLetterUppercase()} Corpse", 1.0)
        }
    }

    // TODO add a check to not add waypoints that are within 5 blocks of another parsed waypoint
    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val message = event.message

        val matcher = partyCorpseCoords.matcher(message)
        if (!matcher.matches()) return

        val name = matcher.group("playerName").cleanPlayerName()
        if (name == Minecraft.getMinecraft().thePlayer.name) return

        val x = matcher.group("x").trim().toInt()
        val y = matcher.group("y").trim().toInt()
        val z = matcher.group("z").trim().toInt()
        val location = LorenzVec(x, y, z)

        parsedLocations.add(location)
    }

    fun isEnabled() = IslandType.MINESHAFT.isInIsland() && config.enabled
}
