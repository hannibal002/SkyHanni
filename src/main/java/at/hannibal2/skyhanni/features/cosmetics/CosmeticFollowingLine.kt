package at.hannibal2.skyhanni.features.cosmetics

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.enums.OutsideSBFeature
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactLocation
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CosmeticFollowingLine {

    private val config get() = SkyHanniMod.feature.gui.cosmetic.followingLine

    private var locations = LinkedHashMap<LorenzVec, LocationSpot>()
    private val latestLocations = LinkedHashMap<LorenzVec, LocationSpot>()

    class LocationSpot(val time: SimpleTimeMark, val onGround: Boolean)

    @HandleEvent
    fun onWorldChange() {
        locations = LinkedHashMap()
    }

    @HandleEvent(onlyOnSkyblockOrFeatures = [OutsideSBFeature.FOLLOWING_LINE])
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled) return

        updateClose(event)

        val firstPerson = PlayerUtils.isFirstPersonView()
        val color = config.lineColor.toColor()

        renderClose(event, firstPerson, color)
        renderFar(event, firstPerson, color)
    }

    private fun renderFar(event: SkyHanniRenderWorldEvent, firstPerson: Boolean, color: Color) {
        val last7 = locations.keys.toList().takeLast(7)
        val last2 = locations.keys.toList().takeLast(2)

        locations.keys.zipWithNext { a, b ->
            locations[b]?.let {
                if (firstPerson && !it.onGround && b in last7) return
                if (b in last2 && it.time.passedSince() < 400.milliseconds) return
                event.draw3DLine(a, b, color, it.getWidth(), !config.behindBlocks)
            }
        }
    }

    private fun updateClose(event: SkyHanniRenderWorldEvent) {
        val playerLocation = event.exactLocation(MinecraftCompat.localPlayer).up(0.3)
        latestLocations[playerLocation] = LocationSpot(SimpleTimeMark.now(), PlayerUtils.onGround())
        latestLocations.values.removeIf { it.time.passedSince() > 600.milliseconds }
    }

    private fun renderClose(event: SkyHanniRenderWorldEvent, firstPerson: Boolean, color: Color) {
        if (firstPerson && latestLocations.any { !it.value.onGround }) return

        latestLocations.keys.zipWithNext { a, b ->
            latestLocations[b]?.let {
                event.draw3DLine(a, b, color, it.getWidth(), !config.behindBlocks)
            }
        }
    }

    private fun LocationSpot.getWidth(): Int {
        val millis = time.passedSince().inWholeMilliseconds
        val percentage = millis.toDouble() / (config.secondsAlive * 1000.0)
        val maxWidth = config.lineWidth
        return (1 + maxWidth - percentage * maxWidth).toInt().coerceAtLeast(1)
    }

    @HandleEvent(onlyOnSkyblockOrFeatures = [OutsideSBFeature.FOLLOWING_LINE])
    fun onTick(event: SkyHanniTickEvent) {
        if (!config.enabled) return

        if (event.isMod(5)) {
            locations.values.removeIf { it.time.passedSince() > config.secondsAlive.seconds }

            // Safety check to not cause lags
            while (locations.size > 5_000) locations.remove(locations.keys.first())
        }

        if (event.isMod(2)) {
            val playerLocation = LocationUtils.playerLocation().up(0.3)
            if (locations.keys.lastOrNull()?.distance(playerLocation)?.let { it < 0.1 } == true) return
            locations[playerLocation] = LocationSpot(SimpleTimeMark.now(), PlayerUtils.onGround())
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(9, "misc.cosmeticConfig", "misc.cosmetic")
        event.move(9, "misc.cosmeticConfig.followingLineConfig", "misc.cosmetic.followingLine")
        event.move(9, "misc.cosmeticConfig.arrowTrailConfig", "misc.cosmetic.arrowTrail")
        event.move(31, "misc.cosmetic", "gui.cosmetic")
    }
}
