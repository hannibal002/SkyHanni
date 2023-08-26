package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.net.URLEncoder

class CrystalHollowsSharedLocations {

    val config get() = SkyHanniMod.feature.misc.mining
    private val logger = LorenzLogger("crystal/locationSharing")

    var locations = mutableListOf<CrystalHollowLocations>()
    var locationsNames = listOf(
        "Mines of Divan",
        "Lost Precursor City",
        "Khazad-dûm",
        "Jungle Temple",
        "Goblin Queen's Den",
        "Fairy Grotto",
        "Dragon's Lair",
        "King's Scent"
    )

    var userAdded = false

    var baseAddress = "https://api.dragon99z.de"
    private var apiKey = "379f3118-wlgt-0649-ni38-9fa5477e1b05"

    private suspend fun coordinationToURL(x: Double, y: Double, z: Double): String {

        val coordinate = """{
                "x":$x,
                "y":$y,
                "z":$z
            }"""

        val encodedCoordinate = withContext(Dispatchers.IO) {
            URLEncoder.encode(coordinate, "UTF-8")
        }

        return encodedCoordinate
    }

    private suspend fun getLocations() {
        if (!isEnabled()) return
        if (!userAdded) return
        val serverId = LorenzUtils.skyBlockServerId
        val url = "$baseAddress/api/coordinates?serverId=$serverId&Key=$apiKey"
        try {
            val result = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }.asJsonObject
            for (coordinatesEntry in result["coordinates"].asJsonArray) {
                val coordinatesArray = coordinatesEntry.asJsonObject
                if (!coordinatesArray["coords"].isJsonNull && !coordinatesArray["location"].isJsonNull) {
                    val coordinates = coordinatesArray["coords"].asJsonObject
                    val name = coordinatesArray["location"].asString
                    val location =
                        LorenzVec(coordinates["x"].asDouble, coordinates["y"].asDouble, coordinates["z"].asDouble)
                    if (locations.isNotEmpty()) {
                        if (locations.any { it.name != name }) {
                            locations.add(CrystalHollowLocations(location, name))
                        }
                    } else {
                        locations.add(CrystalHollowLocations(location, name))
                    }
                }
            }

        } catch (e: Exception) {
            println("url: '$url'")
            e.printStackTrace()
        }
    }

    private suspend fun addUser() {
        if (!isEnabled()) return
        if (userAdded) return
        val serverId = LorenzUtils.skyBlockServerId
        val uuid = LorenzUtils.getPlayerUuid()
        val url = "$baseAddress/api/addUser?serverId=$serverId&userId=$uuid&Key=$apiKey"
        try {
            val result = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }.asJsonObject
            val success = result["success"].asBoolean
            if (success) {
                userAdded = true
                logger.log("Successful added $serverId and $uuid to the server")
            }

        } catch (e: Exception) {
            println("url: '$url'")
            e.printStackTrace()
        }
    }

    private suspend fun removeUser() {
        if (!userAdded) return
        val serverId = LorenzUtils.skyBlockServerId
        val uuid = LorenzUtils.getPlayerUuid()
        val url = "$baseAddress/api/removeUser?serverId=$serverId&userId=$uuid&Key=$apiKey"
        try {
            val result = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }.asJsonObject
            val success = result["success"].asBoolean
            if (success) {
                userAdded = false
                logger.log("Successful removed $serverId and $uuid to the server")
            }

        } catch (e: Exception) {
            println("url: '$url'")
            e.printStackTrace()
        }
    }

    private suspend fun addCoordinates(location: String) {
        if (!isEnabled()) return
        if (!userAdded) return
        if (!locationsNames.contains(location)) return
        if (locations.any { it.name == location }) return

        val serverId = LorenzUtils.skyBlockServerId
        val uuid = LorenzUtils.getPlayerUuid()
        val encodedLocation = withContext(Dispatchers.IO) {
            URLEncoder.encode(location, "UTF-8")
        }
        val coordinate = LocationUtils.playerLocation()

        val url = "$baseAddress/api/save?serverId=$serverId&userId=$uuid&location=$encodedLocation&coordinates=${
            coordinationToURL(
                coordinate.x, coordinate.y, coordinate.z
            )
        }&Key=$apiKey"
        try {
            val result = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }.asJsonObject
            val success = result["success"].asBoolean
            if (success) {
                logger.log("Successful send $location to the server")
            }
        } catch (e: Exception) {
            println("url: '$url'")
            e.printStackTrace()
        }
    }

    private fun update(location: String) {
        SkyHanniMod.coroutineScope.launch {
            addUser()
            getLocations()
            addCoordinates(location)
        }
    }

    @SubscribeEvent
    fun onTimer(event: LorenzTickEvent) {
        if (!event.isMod(40)) return
        if (!isEnabled()) return

        val location = LorenzUtils.skyBlockArea
        update(location)
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        val locationsCopy = locations.toList()
        if (locationsCopy.isNotEmpty()) {
            for ((location, name) in locationsCopy) {
                event.drawColor(location, LorenzColor.DARK_BLUE, alpha = 1f)
                event.drawWaypointFilled(location, LorenzColor.BLUE.toColor(), seeThroughBlocks = true, beacon = true)
                event.drawString(location.add(0.5, 0.5, 0.5), "§b$name", true)
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        locations.clear()
        if (userAdded) {
            SkyHanniMod.coroutineScope.launch {
                removeUser()
            }
        }
        logger.log("Reset everything (world change)")
    }

    data class CrystalHollowLocations(val location: LorenzVec, val name: String)

    fun isEnabled() = IslandType.CRYSTAL_HOLLOWS.isInIsland() && config.crystalHollowsShareLocations

}