package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.garden.pests.PestUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayerIgnoreY
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.ParticlePathBezierFitter
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactPlayerEyeLocation
import com.google.gson.JsonPrimitive
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.core.particles.ParticleTypes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PestParticleWaypoint {

    private val config get() = SkyHanniMod.feature.garden.pests.pestWaypoint

    private val bezierFitter = ParticlePathBezierFitter(3)

    private var lastPestTrackerUse = SimpleTimeMark.farPast()
    private var lastParticle = SimpleTimeMark.farPast()

    private var guessPosition: LorenzVec? = null
    private var isGuessPlotMiddle: Boolean = false

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onItemClick(event: ItemClickEvent) {
        if (!isEnabled() || !PestApi.hasVacuumInHand()) return
        if (event.clickType != ClickType.LEFT_CLICK) return
        if (MinecraftCompat.localPlayer.isShiftKeyDown) return
        reset()
        lastPestTrackerUse = SimpleTimeMark.now()
    }

    @HandleEvent(priority = HandleEvent.LOW, receiveCancelled = true, onlyOnIsland = IslandType.GARDEN)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return

        if (config.hideParticles && event.type == ParticleTypes.FIREWORK) event.cancel()

        if (lastPestTrackerUse.passedSince() > 5.seconds) return
        when {
            event.isEnchantmentTable() -> {
                if (config.hideParticles) event.cancel()
                return
            }

            !event.isVillagerAngry() -> return
        }
        if (config.hideParticles) event.cancel()

        lastParticle = SimpleTimeMark.now()
        val pos = event.location

        if (bezierFitter.isEmpty()) {
            bezierFitter.addPoint(pos)
            return
        }

        val lastPoint = bezierFitter.getLastPoint() ?: return
        val dist = lastPoint.distance(pos)
        if (dist == 0.0 || dist > 3.0) return
        bezierFitter.addPoint(pos)

        val solved = bezierFitter.solve() ?: return
        guessPosition = solved
        isGuessPlotMiddle = GardenPlotApi.getPlot(solved)?.middle?.equalsIgnoreY(solved.ceil()) ?: false
    }

    private fun ReceiveParticleEvent.isEnchantmentTable(): Boolean =
        type == ParticleTypes.ENCHANT && count == 10 && speed == -2f && offset.isZero()

    private fun ReceiveParticleEvent.isVillagerAngry(): Boolean =
        type == ParticleTypes.ANGRY_VILLAGER && count == 1 && speed == 0f && offset.isZero()

    @HandleEvent
    fun onWorldChange() = reset()

    private fun reset() {
        lastPestTrackerUse = SimpleTimeMark.farPast()
        guessPosition = null
        isGuessPlotMiddle = false
        bezierFitter.reset()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (bezierFitter.isEmpty()) return
        if (lastPestTrackerUse.passedSince() > config.showForSeconds.seconds) {
            reset()
            return
        }
        val waypoint = guessPosition ?: return
        val distance = waypoint.distance(event.exactPlayerEyeLocation())
        val color: ChromaColour
        if (isGuessPlotMiddle && config.differentiatePlotMiddle) {
            color = LorenzColor.YELLOW.toChromaColor()
            event.drawDynamicText(waypoint, " §r§e(plot middle)", 1.0, (-0.1 - distance / (12 * 1.7)).toFloat())
        } else {
            color = LorenzColor.RED.toChromaColor()
        }

        event.drawWaypointFilled(waypoint, color.toColor(), beacon = true)
        event.drawDynamicText(waypoint, "§aPest Guess", 1.3)
        if (config.drawLine) {
            event.drawLineToEye(
                waypoint.add(0.5, 0.5, 0.5),
                color,
                3,
                false,
            )
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick() {
        if (!isEnabled()) return
        val guessPoint = guessPosition ?: return

        if (guessPoint.distanceToPlayerIgnoreY() > 8) return
        if (lastPestTrackerUse.passedSince() !in 1.seconds..config.showForSeconds.seconds) return
        reset()
    }

    @HandleEvent(PestUpdateEvent::class)
    fun onPestUpdate() {
        if (PestApi.scoreboardPests == 0) reset()
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(104, "garden.pests.pestWaypoint.enabled") {
            JsonPrimitive(true)
        }
    }

    private fun isEnabled() = config.enabled

}
