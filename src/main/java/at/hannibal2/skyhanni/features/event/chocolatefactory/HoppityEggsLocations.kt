package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.expand
import net.minecraft.item.ItemStack
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object HoppityEggsLocations {

    private val config get() = ChocolateFactoryApi.config.hoppityEggs

    private val locatorItem = "EGGLOCATOR".asInternalName()

    private var lastParticlePosition: LorenzVec? = null
    private val validParticleLocations = mutableListOf<LorenzVec>()

    private var drawLocations = false
    private var firstPos = LorenzVec()
    private var secondPos = LorenzVec()
    private val possibleEggLocations = mutableListOf<LorenzVec>()

    private var ticksSinceLastParticleFound = -1
    private var lastGuessMade = SimpleTimeMark.farPast()

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
        possibleEggLocations.clear()
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
        if (sharedEggLocation != null) {
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
        HoppityEggsLocations.lastParticlePosition = null
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
        val islandEggsLocations = getCurrentIslandEggLocations() ?: return
        val listSize = validParticleLocations.size
        if (listSize < 5) return

        val secondPoint = validParticleLocations.removeLast()
        val firstPoint = validParticleLocations.removeLast()

        val xDiff = secondPoint.x - firstPoint.x
        val yDiff = secondPoint.y - firstPoint.y
        val zDiff = secondPoint.z - firstPoint.z

        firstPos = firstPoint.roundLocationToBlock()

        secondPos = LorenzVec(
            secondPoint.x + xDiff * 1000,
            secondPoint.y + yDiff * 1000,
            secondPoint.z + zDiff * 1000
        ).roundLocationToBlock()

        possibleEggLocations.clear()

        val sortedEggs = islandEggsLocations.filter {
            it.getEggLocationWeight(firstPos, secondPos) < 1
        }.sortedBy {
            it.getEggLocationWeight(firstPos, secondPos)
        }

        val maxLineDistance = sortedEggs.sortedByDescending {
            it.nearestPointOnLine(firstPos, secondPos).distance(firstPos)
        }
        if (maxLineDistance.isEmpty()) {
            LorenzUtils.sendTitle("§cNo eggs found, try getting closer", 2.seconds)
            return
        }
        secondPos = maxLineDistance.first().nearestPointOnLine(firstPos, secondPos)

        possibleEggLocations.addAll(sortedEggs)

        drawLocations = true
    }

    private fun getWorldBoundingBox(islandEggs: List<LorenzVec>): AxisAlignedBB {
        var minX = 10000.0
        var minY = 10000.0
        var minZ = 10000.0
        var maxX = -10000.0
        var maxY = -10000.0
        var maxZ = -10000.0
        for (eggLocation in islandEggs) {
            if (eggLocation.x < minX) minX = eggLocation.x
            if (eggLocation.y < minY) minY = eggLocation.y
            if (eggLocation.z < minZ) minZ = eggLocation.z
            if (eggLocation.x > maxX) maxX = eggLocation.x
            if (eggLocation.y > maxY) maxY = eggLocation.y
            if (eggLocation.z > maxZ) maxZ = eggLocation.z
        }
        return AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ).expand(10.0)
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
        && ChocolateFactoryApi.isHoppityEvent()

    private val ItemStack.isLocatorItem get() = getInternalName() == locatorItem
    private fun hasLocatorInInventory() = InventoryUtils.getItemsInOwnInventory().any { it.isLocatorItem }

    private fun LorenzVec.getEggLocationWeight(firstPoint: LorenzVec, secondPoint: LorenzVec): Double {
        val distToLine = this.distanceToLine(firstPoint, secondPoint)
        val distToStart = this.distance(firstPoint)
        val distMultiplier = distToStart * 2 / 100 + 3
        val disMultiplierSquared = distMultiplier * distMultiplier
        return distToLine / disMultiplierSquared
    }
}
