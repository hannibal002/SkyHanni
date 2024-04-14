package at.hannibal2.skyhanni.features.misc.chocolatefactory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.HoppityEggLocationsJson
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils.clampTo
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.expand
import net.minecraft.item.ItemStack
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object EggLocations {

    private val config get() = SkyHanniMod.feature.misc.chocolateFactory

    private val locatorItem = "EGGLOCATOR".asInternalName()

    private var lastParticlePosition: LorenzVec? = null
    private val validParticleLocations = mutableListOf<LorenzVec>()

    private var drawLocations = false
    private var firstPos = LorenzVec()
    private var secondPos = LorenzVec()
    private val possibleEggLocations = mutableListOf<LorenzVec>()

    private var eggLocations: Map<IslandType, List<LorenzVec>> = mapOf()
    private var ticksSinceLastParticleFound = -1

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<HoppityEggLocationsJson>("HoppityEggLocations")

        eggLocations = data.eggLocations
    }

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
    }

    private fun getCurrentIslandEggLocations(): List<LorenzVec>? {
        return eggLocations[LorenzUtils.skyBlockIsland]
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return

        event.draw3DLine(firstPos, secondPos, LorenzColor.RED.toColor(), 2, false)

        if (drawLocations) {
            for ((index, eggLocation) in possibleEggLocations.withIndex()) {
                event.drawWaypointFilled(
                    eggLocation,
                    LorenzColor.GREEN.toColor(),
                    seeThroughBlocks = true,
                )
                event.drawDynamicText(eggLocation.add(y = 1), "§aEgg $index", 1.5)
            }
        }

        // if no locator draw all waypoints
    }

    fun eggFound() {
        resetData()
    }

    fun shareNearbyEggLocation(playerLocation: LorenzVec, meal: String) {
        val islandEggsLocations = getCurrentIslandEggLocations() ?: return
        val closestEgg = islandEggsLocations.minByOrNull { it.distance(playerLocation) } ?: return

        val x = closestEgg.x.toInt()
        val y = closestEgg.y.toInt()
        val z = closestEgg.z.toInt()

        val message = "[SkyHanni] $meal Chocolate Egg located at x: $x, y: $y, z: $z"
        ChatUtils.sendCommandToServer("ac $message")
    }

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (!hasLocatorInInventory()) return
        if (!event.isVillagerParticle() && !event.isEnchantmentParticle()) return

        val lastParticlePosition = lastParticlePosition ?: run {
            this.lastParticlePosition = event.location
            return
        }
        if (lastParticlePosition == event.location) {
            validParticleLocations.add(event.location)
            ticksSinceLastParticleFound = 0
        }
        this.lastParticlePosition = null
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

        val worldBoundingBox = getWorldBoundingBox(islandEggsLocations)

        val boundingBox = AxisAlignedBB(
            firstPos.x, firstPos.y, firstPos.z,
            secondPos.x, secondPos.y, secondPos.z
        ).expand(5.0).clampTo(worldBoundingBox)

        possibleEggLocations.clear()
        // todo allow more leeway for further points later
        val a = islandEggsLocations.filter {
            it.distanceToLine(firstPos, secondPos) < 200.0
        }

        possibleEggLocations.addAll(a.sortedBy {
            it.distanceToLine(firstPos, secondPos)
        }
        )

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

    private fun ReceiveParticleEvent.isVillagerParticle() =
        type == EnumParticleTypes.VILLAGER_HAPPY && speed == 0.0f && count == 1

    private fun ReceiveParticleEvent.isEnchantmentParticle() =
        type == EnumParticleTypes.ENCHANTMENT_TABLE && speed == -2.0f && count == 10

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.waypointsEnabled
    // todo eeeeeeeeeeeeeeeeeeeeeeeeeeeeeee
//         && SkyblockSeason.getCurrentSeason() == SkyblockSeason.SPRING

    private val ItemStack.isLocatorItem get() = getInternalName() == locatorItem
    private fun hasLocatorInInventory() = InventoryUtils.getItemsInOwnInventory().any { it.isLocatorItem }
}
