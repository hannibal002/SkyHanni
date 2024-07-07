package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.RenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HoppityEggLocator {
    private val config get() = HoppityEggsManager.config

    private val locatorItem = "EGGLOCATOR".asInternalName()

    private var lastParticlePosition: LorenzVec? = null
    private var lastParticlePositionForever: LorenzVec? = null
    private var lastChange = SimpleTimeMark.farPast()
    private var lastClick = SimpleTimeMark.farPast()
    private val validParticleLocations = mutableListOf<LorenzVec>()

    private var drawLocations = false
    private var firstPos = LorenzVec()
    private var secondPos = LorenzVec()

    private var ticksSinceLastParticleFound = -1
    private var lastGuessMade = SimpleTimeMark.farPast()
    private var eggLocationWeights = listOf<Double>()

    var sharedEggLocation: LorenzVec? = null
    var possibleEggLocations = listOf<LorenzVec>()
    var currentEggType: HoppityEggType? = null
    var currentEggNote: String? = null

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        resetData()
    }

    private fun resetData() {
        validParticleLocations.clear()
        ticksSinceLastParticleFound = -1
        possibleEggLocations = emptyList()
        firstPos = LorenzVec()
        secondPos = LorenzVec()
        drawLocations = false
        sharedEggLocation = null
        currentEggType = null
        currentEggNote = null
        lastParticlePosition = null
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return

        event.drawGuessImmediately()

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

    private fun LorenzRenderWorldEvent.drawGuessLocations() {
        val eyeLocation = exactPlayerEyeLocation()
        for ((index, eggLocation) in possibleEggLocations.withIndex()) {
            drawEggWaypoint(eggLocation, "§aGuess #${index + 1}")
            if (config.showLine) {
                draw3DLine(eyeLocation, eggLocation.add(0.5, 0.5, 0.5), LorenzColor.GREEN.toColor(), 2, false)
            }
        }
    }

    private fun LorenzRenderWorldEvent.drawDuplicateEggs(islandEggsLocations: Set<LorenzVec>) {
        if (!config.highlightDuplicateEggLocations || !config.showNearbyDuplicateEggLocations) return
        for (eggLocation in islandEggsLocations) {
            val dist = eggLocation.distanceToPlayer()
            if (dist < 10 && HoppityEggLocations.hasCollectedEgg(eggLocation)) {
                val alpha = ((10 - dist) / 10).coerceAtMost(0.5).toFloat()
                drawColor(eggLocation, LorenzColor.RED, false, alpha)
                drawDynamicText(eggLocation.add(y = 1), "§cDuplicate Location!", 1.5)
            }
        }
    }

    private fun LorenzRenderWorldEvent.drawGuessImmediately() {
        if (config.waypointsImmediately && lastClick.passedSince() < 5.seconds) {
            lastParticlePositionForever?.let {
                if (lastChange.passedSince() < 300.milliseconds) {
                    val eyeLocation = exactPlayerEyeLocation()
                    if (eyeLocation.distance(it) > 2) {
                        drawWaypointFilled(
                            it,
                            LorenzColor.GREEN.toColor(),
                            seeThroughBlocks = true,
                        )
                        drawDynamicText(it.add(y = 1), "§aGuess", 1.5)
                    }
                    if (!drawLocations && config.showLine) {
                        draw3DLine(eyeLocation, it.add(0.5, 0.5, 0.5), LorenzColor.GREEN.toColor(), 2, false)
                    }
                }
            }
        }
    }

    private fun LorenzRenderWorldEvent.drawEggWaypoint(location: LorenzVec, label: String) {
        val shouldMarkDuplicate = config.highlightDuplicateEggLocations
            && HoppityEggLocations.hasCollectedEgg(location)
        val possibleDuplicateLabel = if (shouldMarkDuplicate) "$label §c(Duplicate Location)" else label
        if (!shouldMarkDuplicate) {
            drawWaypointFilled(location, LorenzColor.GREEN.toColor(), seeThroughBlocks = true)
        } else {
            drawColor(location, LorenzColor.RED.toColor(), false, 0.5f)
        }
        drawDynamicText(location.add(y = 1), possibleDuplicateLabel, 1.5)
    }

    private fun shouldShowAllEggs() =
        config.showAllWaypoints && !hasLocatorInHotbar() && HoppityEggType.eggsRemaining()

    fun eggFound() {
        resetData()
    }

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (!hasLocatorInHotbar()) return
        if (!event.isVillagerParticle() && !event.isEnchantmentParticle()) return

        val lastParticlePosition = lastParticlePosition ?: run {
            lastParticlePosition = event.location
            lastParticlePositionForever = lastParticlePosition
            lastChange = SimpleTimeMark.now()
            return
        }
        if (lastParticlePosition == event.location) {
            validParticleLocations.add(event.location)
            ticksSinceLastParticleFound = 0
        }
        HoppityEggLocator.lastParticlePosition = null
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (validParticleLocations.isEmpty()) return
        ticksSinceLastParticleFound++

        if (ticksSinceLastParticleFound < 6) return

        calculateEggPosition()

        ticksSinceLastParticleFound = 0
        validParticleLocations.clear()
        lastParticlePosition = null
    }

    @SubscribeEvent
    fun onItemClick(event: ItemClickEvent) {
        if (!isEnabled()) return
        val item = event.itemInHand ?: return

        if (event.clickType == ClickType.RIGHT_CLICK && item.isLocatorItem) {
            lastClick = SimpleTimeMark.now()
            MythicRabbitPetWarning.check()
        }
    }

    private fun calculateEggPosition() {
        if (lastGuessMade.passedSince() < 1.seconds) return
        lastGuessMade = SimpleTimeMark.now()
        possibleEggLocations = emptyList()

        val islandEggsLocations = HoppityEggLocations.islandLocations
        val listSize = validParticleLocations.size

        if (listSize < 5) return

        val secondPoint = validParticleLocations.removeLast()
        firstPos = validParticleLocations.removeLast()

        val xDiff = secondPoint.x - firstPos.x
        val yDiff = secondPoint.y - firstPos.y
        val zDiff = secondPoint.z - firstPos.z

        secondPos = LorenzVec(
            secondPoint.x + xDiff * 1000,
            secondPoint.y + yDiff * 1000,
            secondPoint.z + zDiff * 1000,
        )

        val sortedEggs = islandEggsLocations.map {
            it to it.getEggLocationWeight(firstPos, secondPos)
        }.sortedBy { it.second }

        eggLocationWeights = sortedEggs.map {
            it.second.round(3)
        }.take(5)

        val filteredEggs = sortedEggs.filter {
            it.second < 1
        }.map { it.first }

        val maxLineDistance = filteredEggs.sortedByDescending {
            it.nearestPointOnLine(firstPos, secondPos).distance(firstPos)
        }

        if (maxLineDistance.isEmpty()) {
            LorenzUtils.sendTitle("§cNo eggs found, try getting closer", 2.seconds)
            return
        }
        secondPos = maxLineDistance.first().nearestPointOnLine(firstPos, secondPos)

        possibleEggLocations = filteredEggs

        drawLocations = true
    }

    fun isValidEggLocation(location: LorenzVec): Boolean =
        HoppityEggLocations.islandLocations.any { it.distance(location) < 5.0 }

    private fun ReceiveParticleEvent.isVillagerParticle() =
        type == EnumParticleTypes.VILLAGER_HAPPY && speed == 0.0f && count == 1

    private fun ReceiveParticleEvent.isEnchantmentParticle() =
        type == EnumParticleTypes.ENCHANTMENT_TABLE && speed == -2.0f && count == 10

    fun isEnabled() = LorenzUtils.inSkyBlock && config.waypoints && !GardenAPI.inGarden() &&
        !ReminderUtils.isBusy(true) && ChocolateFactoryAPI.isHoppityEvent()

    private val ItemStack.isLocatorItem get() = getInternalName() == locatorItem

    private fun hasLocatorInHotbar() = RecalculatingValue(1.seconds) {
        LorenzUtils.inSkyBlock && InventoryUtils.getItemsInHotbar().any { it.isLocatorItem }
    }.getValue()

    private fun LorenzVec.getEggLocationWeight(firstPoint: LorenzVec, secondPoint: LorenzVec): Double {
        val distToLine = this.distanceToLine(firstPoint, secondPoint)
        val distToStart = this.distance(firstPoint)
        val distMultiplier = distToStart * 2 / 100 + 5
        val disMultiplierSquared = distMultiplier * distMultiplier
        return distToLine / disMultiplierSquared
    }

    @SubscribeEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Hoppity Eggs Locations")

        if (!isEnabled()) {
            event.addIrrelevant("not in skyblock or waypoints are disabled")
            return
        }

        event.addIrrelevant {
            add("First Pos: $firstPos")
            add("Second Pos: $secondPos")
            add("Possible Egg Locations: ${possibleEggLocations.size}")
            add("Egg Location Weights: $eggLocationWeights")
            add("Last Time Checked: ${lastGuessMade.passedSince().inWholeSeconds}s ago")
            add("Draw Locations: $drawLocations")
            add("Shared Egg Location: ${sharedEggLocation ?: "None"}")
            add("Current Egg Type: ${currentEggType ?: "None"}")
            add("Current Egg Note: ${currentEggNote ?: "None"}")
        }
    }
}
