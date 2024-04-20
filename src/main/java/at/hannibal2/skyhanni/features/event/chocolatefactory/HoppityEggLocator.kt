package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object HoppityEggLocator {

    private val config get() = ChocolateFactoryAPI.config.hoppityEggs

    private val locatorItem = "EGGLOCATOR".asInternalName()

    private var lastParticlePosition: LorenzVec? = null
    private val validParticleLocations = mutableListOf<LorenzVec>()

    private var drawLocations = false
    private var firstPos = LorenzVec()
    private var secondPos = LorenzVec()
    private var possibleEggLocations = listOf<LorenzVec>()

    private var ticksSinceLastParticleFound = -1
    private var lastGuessMade = SimpleTimeMark.farPast()
    private var eggLocationWeights = listOf<Double>()

    var sharedEggLocation: LorenzVec? = null
    var currentEggType: HoppityEggType? = null

    var eggLocations: Map<IslandType, List<LorenzVec>> = mapOf()

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
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return

        event.draw3DLine(firstPos, secondPos, LorenzColor.RED.toColor(), 2, false)

        if (drawLocations) {
            for ((index, eggLocation) in possibleEggLocations.withIndex()) {
                val eggLabel = "§aGuess #${index + 1}"
                event.drawWaypointFilled(
                    eggLocation,
                    LorenzColor.GREEN.toColor(),
                    seeThroughBlocks = true,
                )
                event.drawDynamicText(eggLocation.add(y = 1), eggLabel, 1.5)
            }
            return
        }

        val sharedEggLocation = sharedEggLocation
        if (sharedEggLocation != null && config.sharedWaypoints) {
            event.drawWaypointFilled(
                sharedEggLocation,
                LorenzColor.GREEN.toColor(),
                seeThroughBlocks = true,
            )
            event.drawDynamicText(sharedEggLocation.add(y = 1), "§aShared Egg", 1.5)
            return
        }

        if (!config.showAllWaypoints) return
        if (hasLocatorInInventory()) return
        if (!HoppityEggType.eggsRemaining()) return

        val islandEggsLocations = getCurrentIslandEggLocations() ?: return
        for (eggLocation in islandEggsLocations) {
            event.drawWaypointFilled(
                eggLocation,
                LorenzColor.GREEN.toColor(),
                seeThroughBlocks = true,
            )
            event.drawDynamicText(eggLocation.add(y = 1), "§aEgg", 1.5)
        }
    }

    fun eggFound() {
        resetData()
    }

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (!hasLocatorInInventory()) return
        if (!event.isVillagerParticle() && !event.isEnchantmentParticle()) return

        val lastParticlePosition = lastParticlePosition ?: run {
            lastParticlePosition = event.location
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
    }

    private fun calculateEggPosition() {
        if (lastGuessMade.passedSince() < 1.seconds) return
        lastGuessMade = SimpleTimeMark.now()
        possibleEggLocations = emptyList()

        val islandEggsLocations = getCurrentIslandEggLocations() ?: return
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
            secondPoint.z + zDiff * 1000
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

    fun getCurrentIslandEggLocations(): List<LorenzVec>? =
        eggLocations[LorenzUtils.skyBlockIsland]

    fun isValidEggLocation(location: LorenzVec): Boolean =
        getCurrentIslandEggLocations()?.any { it.distance(location) < 5.0 } ?: false

    private fun ReceiveParticleEvent.isVillagerParticle() =
        type == EnumParticleTypes.VILLAGER_HAPPY && speed == 0.0f && count == 1

    private fun ReceiveParticleEvent.isEnchantmentParticle() =
        type == EnumParticleTypes.ENCHANTMENT_TABLE && speed == -2.0f && count == 10

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.waypoints
        && ChocolateFactoryAPI.isHoppityEvent()

    private val ItemStack.isLocatorItem get() = getInternalName() == locatorItem

    fun hasLocatorInInventory() = RecalculatingValue(1.seconds) {
        if (!LorenzUtils.inSkyBlock) return@RecalculatingValue false
        InventoryUtils.getItemsInOwnInventory().any { it.isLocatorItem }
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

        event.addData {
            add("First Pos: $firstPos")
            add("Second Pos: $secondPos")
            add("Possible Egg Locations: ${possibleEggLocations.size}")
            add("Egg Location Weights: $eggLocationWeights")
            add("Last Time Checked: ${lastGuessMade.passedSince().inWholeSeconds}s ago")
            add("Draw Locations: $drawLocations")
            add("Shared Egg Location: ${sharedEggLocation ?: "None"}")
            add("Current Egg Type: ${currentEggType ?: "None"}")
        }
    }
}
