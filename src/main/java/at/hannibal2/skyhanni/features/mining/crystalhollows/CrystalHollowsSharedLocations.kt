package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.MiscConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.EntityUtils.getSkinTexture
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.monster.EntityMagmaCube
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.net.URLEncoder

class CrystalHollowsSharedLocations {

    val config: MiscConfig.MiningConfig get() = SkyHanniMod.feature.misc.mining
    private val logger = LorenzLogger("crystal/locationSharing")

    var locations = listOf<CrystalHollowLocations>()
    private var locationsNames = listOf(
        "Mines of Divan",
        "Lost Precursor City",
        "Khazad-dûm",
        "Jungle Temple",
        "Goblin Queen's Den",
        "Dragon's Lair"
    )

    enum class CrystalHollowNPCSkins(val skin: String, var found: Boolean = false) {
        MINES_OF_DIVAN_KEEPER("ewogICJ0aW1lc3RhbXAiIDogMTYyNTY3OTY3MTE1MCwKICAicHJvZmlsZUlkIiA6ICJmNDY0NTcxNDNkMTU0ZmEwOTkxNjBlNGJmNzI3ZGNiOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZWxhcGFnbzA1IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzM0MDZlM2Q4OWM1MGFjNzIwZTRiNDVlYWVlNTBkMzBlNDE2YTdkNmU5YWY2MmVkNDg2M2QzY2FmYjFlODBkYjkiCiAgICB9CiAgfQp9"),
        KING_YOLKAR("ewogICJ0aW1lc3RhbXAiIDogMTYyNDU0NjE4NTExNCwKICAicHJvZmlsZUlkIiA6ICIwZjczMDA3NjEyNGU0NGM3YWYxMTE1NDY5YzQ5OTY3OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJPcmVfTWluZXIxMjMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzU2MTIxMGUyMzY5NzdmMmY0ZTJiNDVhNDY0YjZiNGQ0OTI2NGJhMTRlNGVhZTgyZjI1YWMzMDlkYTYyMjhkNCIKICAgIH0KICB9Cn0="),
        TEMPLE_GUARDIAN("ewogICJ0aW1lc3RhbXAiIDogMTYxOTE4MzY4ODY1MSwKICAicHJvZmlsZUlkIiA6ICI3MzgyZGRmYmU0ODU0NTVjODI1ZjkwMGY4OGZkMzJmOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJJb3lhbCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kNjU4NzA2MTliMGMyZGI4MjI0NzlkMmNiMzgyMTI2YmQ3OTljNTgyMzc4YjViMWRmOTNlNzViN2QwMTU1MWI3IgogICAgfQogIH0KfQ=="),
        PROF_ROBOT("ewogICJ0aW1lc3RhbXAiIDogMTYyNTY3OTYyNDMzMCwKICAicHJvZmlsZUlkIiA6ICI3NTE0NDQ4MTkxZTY0NTQ2OGM5NzM5YTZlMzk1N2JlYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGFua3NNb2phbmciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWRhZWNkMTJhMTdiZTQ0YmUwYTNlZDI4NWYyM2MzYTMwODdiNWVkOWM4NGQzMzAxOWU2ZTVhNjhmYjc3ODVlMiIKICAgIH0KICB9Cn0="),
        ;
    }

    private var userAdded = false

    private var baseAddress = "https://api.dragon99z.de"
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
        LorenzUtils.debug("CH sharing getLocations")
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
                        if (!locations.any { it.name != name }) {
                            return
                        }
                    }
                    locations = locations.editCopy {
                        add(CrystalHollowLocations(location, name))
                    }
                }
            }

        } catch (e: Exception) {
            CopyErrorCommand.logError(
                Exception("Error in getLocations with url: '$url'", e),
                "Error while fetching ch waypoints from api.dragon99z.de"
            )
        }
    }

    private suspend fun addUser() {
        LorenzUtils.debug("CH sharing addUser")
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
            CopyErrorCommand.logError(
                Exception("Error in addUser with url '$url'", e),
                "Error while trying to add the user for ch waypoints to api.dragon99z.de"
            )
        }
    }

    private suspend fun removeUser() {
        LorenzUtils.debug("CH sharing removeUser")
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
            CopyErrorCommand.logError(
                Exception("Error in removeUser with uuid: '$uuid', url: '$url'", e),
                "Error while trying to remove the user for ch waypoints to api.dragon99z.de"
            )
        }
    }

    private suspend fun addCoordinates(location: String, coordinate: LorenzVec) {
        LorenzUtils.debug("CH sharing addCoordinates")
        if (!isEnabled()) return
        if (!userAdded) return

        val serverId = LorenzUtils.skyBlockServerId
        val uuid = LorenzUtils.getPlayerUuid()
        val encodedLocation = withContext(Dispatchers.IO) {
            URLEncoder.encode(location, "UTF-8")
        }

        val url = "$baseAddress/api/addLocation?serverId=$serverId&userId=$uuid&location=$encodedLocation&coordinates=${
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
            CopyErrorCommand.logError(
                Exception(
                    "Error in addCoordinates with location: '$location', coordinate: '$coordinate', url: '$url'",
                    e
                ),
                "Error while trying to share a ch waypoint to api.dragon99z.de"
            )
        }
    }

    private suspend fun removeLocation(location: String) {
        LorenzUtils.debug("CH sharing removeLocation")
        if (!isEnabled()) return
        if (!userAdded) return
        if (!locationsNames.contains(location)) return
        if (locations.any { it.name != location }) return

        val serverId = LorenzUtils.skyBlockServerId
        val uuid = LorenzUtils.getPlayerUuid()
        val encodedLocation = withContext(Dispatchers.IO) {
            URLEncoder.encode(location, "UTF-8")
        }

        val url =
            "$baseAddress/api/removeLocation?serverId=$serverId&userId=$uuid&location=$encodedLocation&Key=$apiKey"
        try {
            val result = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }.asJsonObject
            val success = result["success"].asBoolean
            if (success) {
                logger.log("Successful removed $location from the server")
            }
        } catch (e: Exception) {
            CopyErrorCommand.logError(
                Exception("Error in removeLocation with location: '$location', url: '$url'", e),
                "Error while trying remove a ch waypoint in api.dragon99z.de"
            )
        }
    }

    private fun remove(location: String) {
        SkyHanniMod.coroutineScope.launch {
            removeLocation(location)
        }
    }

    private fun update(location: String, coordinate: LorenzVec) {
        if (!locationsNames.contains(location)) return
        if (locations.any { it.name == location }) return
        SkyHanniMod.coroutineScope.launch {
            addUser()
            getLocations()
            addCoordinates(location, coordinate)
        }
    }

    @SubscribeEvent
    fun onTimer(event: LorenzTickEvent) {
        if (!event.isMod(40)) return
        if (!isEnabled()) {
            if (userAdded) {
                SkyHanniMod.coroutineScope.launch {
                    removeUser()
                }
                CrystalHollowNPCSkins.entries.forEach { it.found = false }
                balFound = false
            }
            return
        }
        val location = LorenzUtils.skyBlockArea
        update(location, LocationUtils.playerLocation())
    }

    private var balFound = false

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!isEnabled()) return
        when (val entity = event.entity) {
            is EntityOtherPlayerMP -> {
                when (entity.getSkinTexture().toString()) {
                    CrystalHollowNPCSkins.MINES_OF_DIVAN_KEEPER.skin -> {
                        if (!CrystalHollowNPCSkins.MINES_OF_DIVAN_KEEPER.found) {
                            tryRemoving("Mines of Divan")

                            update("Mines of Divan", entity.getLorenzVec().add(33, 18, 3))
                            CrystalHollowNPCSkins.MINES_OF_DIVAN_KEEPER.found = true
                        }
                    }

                    CrystalHollowNPCSkins.KING_YOLKAR.skin -> {
                        if (!CrystalHollowNPCSkins.KING_YOLKAR.found) {
                            update("King's Scent", entity.getLorenzVec())
                            CrystalHollowNPCSkins.KING_YOLKAR.found = true
                        }
                    }

                    CrystalHollowNPCSkins.TEMPLE_GUARDIAN.skin -> {
                        if (!CrystalHollowNPCSkins.TEMPLE_GUARDIAN.found) {
                            tryRemoving("Jungle Temple")

                            update("Jungle Temple", entity.getLorenzVec())
                            CrystalHollowNPCSkins.TEMPLE_GUARDIAN.found = true
                        }
                    }

                    CrystalHollowNPCSkins.PROF_ROBOT.skin -> {
                        if (!CrystalHollowNPCSkins.PROF_ROBOT.found) {
                            tryRemoving("Lost Precursor City")

                            update("Lost Precursor City", entity.getLorenzVec())
                            CrystalHollowNPCSkins.PROF_ROBOT.found = true
                        }
                    }
                }
            }

            is EntityMagmaCube -> {
                if (LorenzUtils.skyBlockArea == "Khazad-dûm") {
                    if (entity.slimeSize == 27 && !balFound) {
                        tryRemoving("Khazad-dûm")

                        update("Khazad-dûm", entity.getLorenzVec())
                        balFound = true
                    }
                }
            }
        }
    }

    private fun tryRemoving(location: String) {
        if (locations.any { it.name == location }) {
            remove(location)
            locations = locations.editCopy {
                removeIf { it.name == location }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!isEnabled()) return
        for ((location, name) in locations) {
            event.drawColor(location, LorenzColor.DARK_BLUE, alpha = 0.5f)
            event.drawWaypointFilled(location, LorenzColor.BLUE.toColor(), seeThroughBlocks = true, beacon = true)
            event.drawString(location.add(0.5, 0.5, 0.5), "§b$name", true)
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        locations = emptyList()
        if (userAdded) {
            SkyHanniMod.coroutineScope.launch {
                removeUser()
            }
        }
        CrystalHollowNPCSkins.entries.forEach { it.found = false }
        balFound = false
        if (config.crystalHollowsShareLocations) {
            logger.log("Reset everything (world change)")
        }
    }

    data class CrystalHollowLocations(val location: LorenzVec, val name: String)

    fun isEnabled() = IslandType.CRYSTAL_HOLLOWS.isInIsland() && config.crystalHollowsShareLocations

}