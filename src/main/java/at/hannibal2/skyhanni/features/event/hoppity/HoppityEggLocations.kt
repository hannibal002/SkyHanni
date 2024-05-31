package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.HoppityEggLocationsJson
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.NeuProfileDataLoadedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object HoppityEggLocations {

    val currentIslandLocations
        get() = legacyEggLocations[LorenzUtils.skyBlockIsland]

    /**
     * Editing this set will modify the profile storage. null iff ProfileSpecificStorage is null.
     */
    private val currentIslandCollectedLocations
        get() = collectedEggLocations?.getOrPut(LorenzUtils.skyBlockIsland) { mutableSetOf() }

    // TODO: only use apiEggLocations
    private var apiEggLocations: Map<IslandType, Map<String, LorenzVec>> = mapOf()
    private var legacyEggLocations: Map<IslandType, Set<LorenzVec>> = mapOf()

    private var collectedEggLocations: MutableMap<IslandType, MutableSet<LorenzVec>>?
        get() = ChocolateFactoryAPI.profileStorage?.collectedEggLocations
        set(value) {
            ChocolateFactoryAPI.profileStorage?.collectedEggLocations = value
        }

    private val collectedLocationCount
        get() = collectedEggLocations?.values?.sumOf { it.size } ?: 0


    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        // TODO: split Chocolate Factory and Hoppity repo data
        val data = event.getConstant<HoppityEggLocationsJson>("HoppityEggLocations")
        apiEggLocations = data.apiEggLocations
        legacyEggLocations = data.eggLocations.mapValues { it.value.toSet() }
    }

    fun saveNearestEgg() {
        val location = currentIslandLocations
            ?.minByOrNull { it.distanceSqToPlayer() } ?: return
        if (location.distanceSqToPlayer() > 100) {
            ErrorManager.skyHanniError(
                "Player far from any known egg location!",
                "island" to LorenzUtils.skyBlockIsland,
                "distanceSqToPlayer" to location.distanceSqToPlayer(),
                "playerLocation" to LocationUtils.playerLocation(),
                "closestKnownEgg" to location,
            )
        }

        currentIslandCollectedLocations?.add(location)
    }


    private var loadedNeuThisProfile = false

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        loadedNeuThisProfile = false
    }

    @SubscribeEvent
    fun onNeuProfileDataLoaded(event: NeuProfileDataLoadedEvent) {
        if (loadedNeuThisProfile || !HoppityEggsManager.config.loadFromNeuPv) return

        // optional chaining to hopefully catch any API responses missing data
        val rawLocations = event.getCurrentPlayerData()?.events?.easter?.rabbits?.collectedLocations ?: return

        val apiCollectedLocations = rawLocations.values.flatten()

        val result = mutableMapOf<IslandType, MutableSet<LorenzVec>>()

        for ((island, locationNameToCoords) in apiEggLocations) {
            val coords = apiCollectedLocations.mapNotNull { locationNameToCoords[it] }
            result[island] = coords.toMutableSet()
        }

        // don't load if the API is unchanged or behind
        if (apiCollectedLocations.size <= collectedLocationCount) return

        ChatUtils.clickableChat(
            message = "Click here to load ${apiCollectedLocations.size} collected egg locations from NEU PV!",
            onClick = {
                collectedEggLocations = result
                ChatUtils.chat("Updated Hoppity egg location data!")
                      },
            oneTimeClick = true
        )
    }


    fun collectedEggsThisIsland() = currentIslandCollectedLocations?.size ?: 0

    fun hasCollectedEgg(location: LorenzVec) =
        currentIslandCollectedLocations?.contains(location) ?: false


    /* Debug logic, for if any API locations are mislabeled or an egg location isn't in the repo */
    private var showEggLocationsDebug = false

    fun toggleDebug() {
        showEggLocationsDebug = !showEggLocationsDebug
        val enabledDisabled = if (showEggLocationsDebug) "§aEnabled" else "§cDisabled"
        ChatUtils.chat("$enabledDisabled hoppity egg location debug viewer.")
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!LorenzUtils.inSkyBlock || !showEggLocationsDebug) return
        val locations = currentIslandLocations ?: return
        val apiLocations = apiEggLocations[LorenzUtils.skyBlockIsland] ?: return
        val collectedLocations = currentIslandCollectedLocations
        for (location in locations) {
            val name = apiLocations.entries.find { it.value == location }?.key
            val isCollected = collectedLocations?.contains(location) == true
            val color = if (isCollected) LorenzColor.GREEN else LorenzColor.RED
            val nameColorCode = (if (name != null) LorenzColor.GREEN else LorenzColor.RED).getChatColor()

            event.drawColor(location, color, false, 0.5f)
            event.drawDynamicText(location.add(y = 0.5), "$nameColorCode$name", 1.2)
            if (location.distanceSqToPlayer() < 100) {
                event.drawDynamicText(location.add(y = 0.5), location.toCleanString(), 1.0, yOff = 12f)
            }

        }
    }



}
