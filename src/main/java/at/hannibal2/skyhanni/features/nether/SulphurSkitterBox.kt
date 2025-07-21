package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.crimsonisle.SulphurSkitterBoxConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawHitbox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.expandBlock
import net.minecraft.init.Blocks
import net.minecraft.util.AxisAlignedBB

@SkyHanniModule
object SulphurSkitterBox {

    private val config get() = SkyHanniMod.feature.fishing.trophyFishing.sulphurSkitterBox
    private var spongeLocations = listOf<LorenzVec>()
    private var closestSponge: LorenzVec? = null
    private var renderBox: AxisAlignedBB? = null
    private const val RADIUS = 4

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        if (event.isMod(5)) {
            calculateClosestSponge()
        }
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        calculateSpongeLocations()
    }

    private fun calculateClosestSponge() {
        val location = spongeLocations.minByOrNull { it.distanceToPlayer() }
        if (location == closestSponge) return
        closestSponge = location
        renderBox = location?.let {
            val pos1 = it.add(-RADIUS, -RADIUS, -RADIUS)
            val pos2 = it.add(RADIUS + 1, RADIUS + 1, RADIUS + 1)
            pos1.axisAlignedTo(pos2).expandBlock()
        }
    }

    private fun calculateSpongeLocations() {
        spongeLocations = BlockUtils.nearbyBlocks(
            LocationUtils.playerLocation(),
            distance = 15,
            filter = Blocks.sponge,
        ).map { it.key }
    }

    @HandleEvent
    fun onWorldChange() {
        spongeLocations = emptyList()
        closestSponge = null
        renderBox = null
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        val location = closestSponge ?: return
        if (location.distanceToPlayer() >= 50) return
        val axis = renderBox ?: return
        val color = config.boxColor.toColor()
        when (config.boxType) {
            SulphurSkitterBoxConfig.BoxType.FULL -> {
                event.drawFilledBoundingBox(
                    axis,
                    color,
                )
            }

            SulphurSkitterBoxConfig.BoxType.WIREFRAME -> {
                event.drawHitbox(axis, color)
            }
        }
    }

    fun isEnabled() =
        IslandType.CRIMSON_ISLE.isCurrent() && config.enabled && (!config.onlyWithRods || FishingApi.holdingLavaRod)

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(24, "crimsonIsle.sulphurSkitterBoxConfig", "fishing.trophyFishing.sulphurSkitterBox")
    }
}
