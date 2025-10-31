package at.hannibal2.hanni.features.nether

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.config.features.crimsonisle.SulphurSkitterBoxConfig
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.features.fishing.FishingApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.BlockUtils
import at.hannibal2.hanni.utils.ColorUtils.toColor
import at.hannibal2.hanni.utils.LocationUtils
import at.hannibal2.hanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawHitbox
import at.hannibal2.hanni.utils.render.WorldRenderUtils.expandBlock
import net.minecraft.init.Blocks
import net.minecraft.util.AxisAlignedBB

@HanniModule
object SulphurSkitterBox {

    private val config get() = HanniMod.feature.fishing.trophyFishing.sulphurSkitterBox
    private var spongeLocations = listOf<LorenzVec>()
    private var closestSponge: LorenzVec? = null
    private var renderBox: AxisAlignedBB? = null
    private const val RADIUS = 4

    @HandleEvent
    fun onTick(event: HanniTickEvent) {
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
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return
        val location = closestSponge ?: return
        if (location.distanceToPlayer() >= 50) return
        val axis = renderBox ?: return
        val color = config.boxColor
        when (config.boxType) {
            SulphurSkitterBoxConfig.BoxType.FULL -> {
                event.drawFilledBoundingBox(
                    axis,
                    color,
                )
            }

            SulphurSkitterBoxConfig.BoxType.WIREFRAME -> {
                event.drawHitbox(axis, color.toColor())
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
