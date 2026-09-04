package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.ParticleEvent
import at.hannibal2.skyhanni.events.hoppity.EggFoundEvent
import at.hannibal2.skyhanni.events.hoppity.EggSpawnedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.ParticlePathBezierFitter
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToCrosshair
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.projectile.FishingHook
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HoppityEggLocator {
    private val config get() = HoppityEggsManager.config
    private val waypointsConfig get() = config.waypoints
    val locatorItem = "EGGLOCATOR".toInternalName()

    private const val MAX_GUESS_DISTANCE = 5.0

    private var lastClick = SimpleTimeMark.farPast()

    private var drawLocations = false

    @Volatile
    private var warningPending = false

    var sharedEggLocation: LorenzVec? = null
    var possibleEggLocations = listOf<LorenzVec>()
    var currentEggType: HoppityEggType? = null
    var currentEggNote: String? = null

    @HandleEvent
    private fun onEggFound(event: EggFoundEvent) {
        if (event.type.isResetting) resetData()
    }

    @HandleEvent
    private fun onWorldChange() {
        resetData()
    }

    private fun resetData() {
        possibleEggLocations = emptyList()
        drawLocations = false
        sharedEggLocation = null
        currentEggType = null
        currentEggNote = null
        bezierFitter.reset()
        warningPending = false
    }

    @HandleEvent
    private fun onEggSpawned(event: EggSpawnedEvent) {
        if (event.eggType == currentEggType) resetData()
    }

    @HandleEvent
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        if (drawLocations) {
            event.drawGuessLocations()
            return
        }

        sharedEggLocation?.let {
            if (waypointsConfig.shared) {
                event.drawEggWaypoint(it, "§aShared Egg")
                return
            }
        }

        var islandEggsLocations = HoppityEggLocations.islandLocations

        if (shouldShowAllEggs()) {
            if (waypointsConfig.hideDuplicates) {
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
            if (waypointsConfig.showLine) {
                drawLineToCrosshair(eggLocation.blockCenter(), LorenzColor.GREEN.toChromaColor(), 2, false)
            }
        }
    }

    private fun SkyHanniRenderWorldEvent.drawDuplicateEggs(islandEggsLocations: Set<LorenzVec>) {
        if (!waypointsConfig.highlightDuplicates) return
        if (!waypointsConfig.showNearbyDuplicates) return
        if (HoppityEggLocations.foundAllOnThisIsland) return

        for (eggLocation in islandEggsLocations) {
            val dist = eggLocation.distanceToPlayer()
            if (dist < 10 && HoppityEggLocations.hasCollectedEgg(eggLocation)) {
                val alpha = ((10 - dist) / 10).coerceAtMost(0.5).toFloat()
                // TODO add chroma color support via config
                drawColor(eggLocation, LorenzColor.RED.toChromaColor(), false, alpha)
                drawDynamicText(eggLocation.up(), "§cDuplicate Location!", 1.5)

            }
        }
    }

    private fun SkyHanniRenderWorldEvent.drawEggWaypoint(location: LorenzVec, label: String) {
        val shouldMarkDuplicate =
            waypointsConfig.highlightDuplicates &&
                HoppityEggLocations.hasCollectedEgg(location) &&
                !HoppityEggLocations.foundAllOnThisIsland

        val possibleDuplicateLabel = if (shouldMarkDuplicate) "$label §c(Duplicate Location)" else label

        if (!shouldMarkDuplicate) {
            drawWaypointFilled(location, waypointsConfig.color.toColor(), seeThroughBlocks = true)
        } else {
            drawColor(location, LorenzColor.RED.toChromaColor(), false, 0.5f)
        }
        drawDynamicText(location.up(), possibleDuplicateLabel, 1.5)
    }

    private fun shouldShowAllEggs() = waypointsConfig.showAll && !locatorInHotbar && HoppityEggType.anyEggsUnclaimed()

    private val bezierFitter = ParticlePathBezierFitter(3)

    @HandleEvent(onlyOnSkyblock = true, receiveCancelled = true)
    private fun onParticle(event: ParticleEvent) {
        if (!isEnabled()) return
        if (!event.isVillagerParticle()) return
        if (lastClick.passedSince() > 5.seconds) return

        val endCondition: (LorenzVec) -> Boolean = { it.getEntitiesNearby<FishingHook>(0.3).any() }
        if (!bezierFitter.tryAdd(event.location, maxDistanceToLast = 3.0, endCondition = endCondition)) return

        val guess = guessEggLocation() ?: return
        possibleEggLocations = listOf(guess)
        drawLocations = true
        warningPending = false
        trySendingGraph()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onItemClick(event: ItemClickEvent) {
        val item = event.itemInHand ?: return
        if (event.clickType != InteractClickType.RIGHT_CLICK || !item.isLocatorItem) return

        if (!isEnabled()) return
        if (lastClick.passedSince() < 5.seconds) return

        val clickTime = SimpleTimeMark.now()
        lastClick = clickTime
        warningPending = true
        MythicRabbitPetWarning.check()
        trySendingGraph()
        bezierFitter.reset()
        DelayedRun.runDelayed(5.5.seconds) { warnIfNoGuessFound(clickTime) }
    }

    /** Tells the player that the run produced nothing usable. Silent for an outdated use and on islands without known eggs. */
    private fun warnIfNoGuessFound(clickTime: SimpleTimeMark) {
        if (lastClick != clickTime) return
        if (!warningPending) return
        if (bezierFitter.isEmpty()) return
        if (HoppityEggLocations.islandLocations.isEmpty()) return
        ChatUtils.chat("Egglocator result was unreliable, please use it again!")
    }

    private fun guessEggLocation(): LorenzVec? {
        val guessLocation = bezierFitter.solve() ?: return null
        if (!SkyBlockUtils.currentIsland.isInBounds(guessLocation)) return null

        val closestEgg = HoppityEggLocations.islandLocations.minByOrNull { it.distanceSq(guessLocation) }

        return closestEgg?.takeIf { it.distance(guessLocation) <= MAX_GUESS_DISTANCE }
    }

    private fun trySendingGraph() {
        if (!waypointsConfig.showPathFinder) return
        val location = possibleEggLocations.firstOrNull() ?: return
        val color = waypointsConfig.color.toColor()

        IslandGraphs.pathFind(location, "Hoppity Egg", color, condition = { waypointsConfig.showPathFinder })
    }

    fun isValidEggLocation(location: LorenzVec): Boolean = HoppityEggLocations.islandLocations.any {
        it.distance(location) < 5.0
    }

    // TODO verify on 26.3
    private fun ParticleEvent.isVillagerParticle() = type == ParticleTypes.HAPPY_VILLAGER && isSpeed(0f) && count == 1

    fun isEnabled() =
        SkyBlockUtils.inSkyBlock && config.waypoints.enabled && !GardenApi.inGarden() && !ReminderUtils.isBusy(true) &&
            HoppityApi.isHoppityEvent()

    private val SafeItemStack.isLocatorItem get() = getInternalName() == locatorItem

    private val locatorInHotbar by RecalculatingValue(1.seconds) {
        SkyBlockUtils.inSkyBlock && InventoryUtils.getItemsInHotbar().any { it.isLocatorItem }
    }

    @HandleEvent
    private fun onDebugDataCollect(event: DebugDataCollectEvent) {
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

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestrabbitpaths") {
            description = "Tests pathfinding to rabbit eggs. Use a number 0-14."
            category = CommandCategory.DEVELOPER_TEST
            argCallback("target", BrigadierArguments.integer()) { target ->
                HoppityEggLocations.apiEggLocations[SkyBlockUtils.currentIsland]?.let {
                    for ((i, location) in it.values.withIndex()) {
                        if (i == target) {
                            IslandGraphs.pathFind(location, "Hoppity Test", condition = { true })
                            return@argCallback
                        }
                    }
                }
            }
        }
    }
}
