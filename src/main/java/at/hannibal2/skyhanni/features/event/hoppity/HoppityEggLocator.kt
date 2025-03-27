package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.hoppity.EggFoundEvent
import at.hannibal2.skyhanni.events.hoppity.EggSpawnedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.ParticlePathBezierFitter
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumParticleTypes
import kotlin.math.sign
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HoppityEggLocator {
    private val config get() = HoppityEggsManager.config
    val locatorItem = "EGGLOCATOR".toInternalName()

    private var lastClick = SimpleTimeMark.farPast()

    private var drawLocations = false

    var sharedEggLocation: LorenzVec? = null
    var possibleEggLocations = listOf<LorenzVec>()
    var currentEggType: HoppityEggType? = null
    var currentEggNote: String? = null

    @HandleEvent
    fun onEggFound(event: EggFoundEvent) {
        if (event.type.isResetting) resetData()
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        resetData()
    }

    private fun resetData() {
        possibleEggLocations = emptyList()
        drawLocations = false
        sharedEggLocation = null
        currentEggType = null
        currentEggNote = null
        bezierFitter.reset()
    }

    @HandleEvent
    fun onEggSpawn(event: EggSpawnedEvent) {
        if (event.eggType == currentEggType) resetData()
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        if (drawLocations) {
            event.drawGuessLocations()
            return
        }

        sharedEggLocation?.let {
            if (config.sharedWaypoints) {
                event.drawEggWaypoint(it, "§aShared Egg")
                return
            }
        }

        var islandEggsLocations = HoppityEggLocations.islandLocations

        if (shouldShowAllEggs()) {
            if (config.hideDuplicateWaypoints) {
                islandEggsLocations = islandEggsLocations.filter {
                    !HoppityEggLocations.hasCollectedEgg(it)
                }.toSet()
            }
            for (eggLocation in islandEggsLocations) {
                event.drawEggWaypoint(eggLocation, "§aEgg")
            }
            return
        }

        event.drawDuplicateEggs(islandEggsLocations)
    }

    private fun SkyHanniRenderWorldEvent.drawGuessLocations() {
        for ((index, eggLocation) in possibleEggLocations.withIndex()) {
            val name = if (possibleEggLocations.size == 1) {
                "§aGuess"
            } else "§aGuess #${index + 1}"
            drawEggWaypoint(eggLocation, name)
            if (config.showLine) {
                drawLineToEye(eggLocation.blockCenter(), LorenzColor.GREEN.toColor(), 2, false)
            }
        }
    }

    private fun SkyHanniRenderWorldEvent.drawDuplicateEggs(islandEggsLocations: Set<LorenzVec>) {
        if (!config.highlightDuplicateEggLocations || !config.showNearbyDuplicateEggLocations) return
        for (eggLocation in islandEggsLocations) {
            val dist = eggLocation.distanceToPlayer()
            if (dist < 10 && HoppityEggLocations.hasCollectedEgg(eggLocation)) {
                val alpha = ((10 - dist) / 10).coerceAtMost(0.5).toFloat()
                drawColor(eggLocation, LorenzColor.RED, false, alpha)
                drawDynamicText(eggLocation.up(), "§cDuplicate Location!", 1.5)
            }
        }
    }

    private fun SkyHanniRenderWorldEvent.drawEggWaypoint(location: LorenzVec, label: String) {
        val shouldMarkDuplicate = config.highlightDuplicateEggLocations && HoppityEggLocations.hasCollectedEgg(location)
        val possibleDuplicateLabel = if (shouldMarkDuplicate) "$label §c(Duplicate Location)" else label
        if (!shouldMarkDuplicate) {
            drawWaypointFilled(location, config.waypointColor.toSpecialColor(), seeThroughBlocks = true)
        } else {
            drawColor(location, LorenzColor.RED.toColor(), false, 0.5f)
        }
        drawDynamicText(location.up(), possibleDuplicateLabel, 1.5)
    }

    private fun shouldShowAllEggs() = config.showAllWaypoints && !locatorInHotbar && HoppityEggType.anyEggsUnclaimed()

    private val bezierFitter = ParticlePathBezierFitter(3)

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (!event.isVillagerParticle()) return
        if (lastClick.passedSince() > 5.seconds) return

        val lastPoint = bezierFitter.getLastPoint()
        if (lastPoint != null) {
            if (lastPoint.distanceSq(event.location) > 9) return
        }

        if (EntityUtils.getEntitiesNearby<EntityFishHook>(event.location, 0.3).any()) {
            return
        }


        bezierFitter.addPoint(event.location)

        val guess = guessEggLocation() ?: return
        possibleEggLocations = listOf(guess)
        drawLocations = true
        if (possibleEggLocations.size == 1) {
            trySendingGraph()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemClick(event: ItemClickEvent) {
        if (!isEnabled()) return
        val item = event.itemInHand ?: return

        if (event.clickType == ClickType.RIGHT_CLICK && item.isLocatorItem && lastClick.passedSince() >= 5.seconds) {
            lastClick = SimpleTimeMark.now()
            MythicRabbitPetWarning.check()
            trySendingGraph()
            bezierFitter.reset()
        }
    }

    private fun guessEggLocation(): LorenzVec? {
        val guessLocation = bezierFitter.solve() ?: return null

        val guessEgg = HoppityEggLocations.islandLocations.sortedWith { a, b ->
            sign(a.distanceSq(guessLocation) - b.distanceSq(guessLocation)).toInt()
        }.firstOrNull()

        return guessEgg
    }

    private fun trySendingGraph() {
        if (!config.showPathFinder) return
        val location = possibleEggLocations.firstOrNull() ?: return

        val color = config.waypointColor.toSpecialColor()

        IslandGraphs.pathFind(location, "Hoppity Egg", color, condition = { config.showPathFinder })
    }

    fun isValidEggLocation(location: LorenzVec): Boolean = HoppityEggLocations.islandLocations.any {
        it.distance(location) < 5.0
    }

    private fun ReceiveParticleEvent.isVillagerParticle() = type == EnumParticleTypes.VILLAGER_HAPPY && speed == 0.0f && count == 1

    fun isEnabled() =
        LorenzUtils.inSkyBlock && config.waypoints && !GardenApi.inGarden() && !ReminderUtils.isBusy(true) && HoppityApi.isHoppityEvent()

    private val ItemStack.isLocatorItem get() = getInternalName() == locatorItem

    private val locatorInHotbar by RecalculatingValue(1.seconds) {
        LorenzUtils.inSkyBlock && InventoryUtils.getItemsInHotbar().any { it.isLocatorItem }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Hoppity Eggs Locations")

        if (!isEnabled()) {
            event.addIrrelevant("not in skyblock or waypoints are disabled")
            return
        }

        event.addIrrelevant {
            add("Possible Egg Locations: ${possibleEggLocations.size}")
            add("Draw Locations: $drawLocations")
            add("Shared Egg Location: ${sharedEggLocation ?: "None"}")
            add("Current Egg Type: ${currentEggType ?: "None"}")
            add("Current Egg Note: ${currentEggNote ?: "None"}")
        }
    }

    private fun testPathfind(args: Array<String>) {
        val target = args[0].formatInt()
        HoppityEggLocations.apiEggLocations[LorenzUtils.skyBlockIsland]?.let {
            for ((i, location) in it.values.withIndex()) {
                if (i == target) {
                    IslandGraphs.pathFind(location, "Hoppity Test", condition = { true })
                    return
                }
            }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shtestrabbitpaths") {
            description = "Tests pathfinding to rabbit eggs. Use a number 0-14."
            category = CommandCategory.DEVELOPER_TEST
            callback { testPathfind(it) }
        }
    }
}
