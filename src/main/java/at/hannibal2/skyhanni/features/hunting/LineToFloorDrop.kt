package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.hunting.HuntingConfig.FloorDropIsland
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.LocationUtils.playerLocation
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.itemType
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToCrosshair
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.world.entity.Display
import net.minecraft.world.item.Items
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object LineToFloorDrop {

    private val config get() = SkyHanniMod.feature.hunting

    private val dropTimeout = 5.seconds

    private val collectCooldown = 3.seconds

    private val floorDrops = TimeLimitedSet<LorenzVec>(dropTimeout)

    private val collectedDrops = TimeLimitedSet<LorenzVec>(collectCooldown)

    private val color = LorenzColor.GREEN.toChromaColor()

    private const val SCAN_INTERVAL_TICKS = 10

    private const val VERTICAL_SEARCH_RANGE = 8.0

    private const val COLLECT_RADIUS = 2.0

    private const val DROP_DISPLAY_COUNT = 3

    private val dropItem = Items.STRING

    @HandleEvent(onlyOnSkyblock = true)
    private fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) {
            floorDrops.clear()
            collectedDrops.clear()
            return
        }
        if (!event.isMod(SCAN_INTERVAL_TICKS)) return

        val playerLocation = playerLocation()
        val lowerY = playerLocation.y - VERTICAL_SEARCH_RANGE
        val upperY = playerLocation.y + VERTICAL_SEARCH_RANGE

        val displays = getEntitiesNearby<Display.ItemDisplay>(config.floorDropScanRadius.toDouble()) { display ->
            val y = display.blockPosition().y.toDouble()
            y in lowerY..upperY && display.itemStack.itemType == dropItem
        }
        val dropLocations = displays.groupBy { it.blockPosition().toLorenzVec() }
        for ((location, group) in dropLocations) {
            if (group.size == DROP_DISPLAY_COUNT && location !in collectedDrops) {
                floorDrops.add(location)
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled()) return
        collectAt(event.position)
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onEntityClick(event: EntityClickEvent) {
        if (!isEnabled()) return
        if (event.clickedEntity is Display.ItemDisplay) {
            collectAt(event.clickedEntity.blockPosition().toLorenzVec())
        }
    }

    @HandleEvent
    private fun onWorldChange() {
        floorDrops.clear()
        collectedDrops.clear()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        val playerEye = event.exactPlayerEyeLocation()
        val nearest = floorDrops.minByOrNull { it.distanceSq(playerEye) } ?: return
        val maxDistance = config.floorDropMaxDistance.toDouble()
        if (nearest.distanceSq(playerEye) <= maxDistance * maxDistance) {
            event.drawLineToCrosshair(nearest.add(x = 0.5, y = 0.25, z = 0.5), color, config.floorDropLineWidth, false)
        }
    }

    private fun collectAt(position: LorenzVec) {
        val nearby = floorDrops.filter { it.distanceSq(position) <= COLLECT_RADIUS * COLLECT_RADIUS }
        for (location in nearby) {
            floorDrops.remove(location)
            collectedDrops.add(location)
        }
    }

    private fun isEnabled() = config.lineToFloorDrop && isSelectedIsland()

    private fun isSelectedIsland(): Boolean = when (SkyBlockUtils.currentIsland) {
        IslandType.GALATEA -> FloorDropIsland.MOONGLADE_MARSH in config.floorDropIslands
        IslandType.TORRHUS_CANYON -> FloorDropIsland.TORRHUS_CANYON in config.floorDropIslands
        IslandType.SAFARI -> FloorDropIsland.CRITTER_SAFARI in config.floorDropIslands
        else -> false
    }
}
