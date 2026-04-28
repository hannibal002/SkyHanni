package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ColdUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.events.mining.GlaciteMineshaftDetectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.ChatFormatting
import java.awt.Color
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MineshaftCaveInTimer {

    private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft.mineshaftTimerConfig

    private val CAVE_IN_DURATION = 60.seconds

    private var caveInTimerStart = SimpleTimeMark.farPast()
    private var firstColdTime = SimpleTimeMark.farPast()
    private var lastColdValue: Int? = null
    private var totalColdGained: Int = 0

    private var display: List<Renderable> = emptyList()

    @HandleEvent(WorldChangeEvent::class)
    fun onWorldChange() {
        caveInTimerStart = SimpleTimeMark.farPast()
        firstColdTime = SimpleTimeMark.farPast()
        lastColdValue = 0
        totalColdGained = 0
        display = emptyList()
    }

    @HandleEvent(GlaciteMineshaftDetectEvent::class)
    fun onMineshaftDetect() {
        caveInTimerStart = SimpleTimeMark.now()
        firstColdTime = SimpleTimeMark.farPast()
        lastColdValue = 0
        totalColdGained = 0
    }

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    fun onColdUpdate(event: ColdUpdateEvent) {
        if (event.cold == 0) return

        val prev = lastColdValue
        lastColdValue = event.cold

        if (prev == null) {
            firstColdTime = SimpleTimeMark.now()
            return
        }

        val delta = event.cold - prev
        if (delta > 0) totalColdGained += delta
    }

    @HandleEvent(SecondPassedEvent::class, onlyOnIsland = IslandType.MINESHAFT)
    fun onSecondPassed() {
        if (!config.enabled || caveInTimerStart.isFarPast()) {
            display = emptyList()
            return
        }

        val timeLeft = CAVE_IN_DURATION - caveInTimerStart.passedSince()
        val caveInText = if (timeLeft.isNegative()) "Caved in!" else timeLeft.format()

        display = buildList {
            val componentBuilder = componentBuilder {
                appendWithColor("Entrance caves in: ", ChatFormatting.WHITE)
                val caveInColor = getColor(timeLeft)
                appendWithColor(caveInText, caveInColor.rgb)
            }
            add(Renderable.text(componentBuilder))

            if (config.showTimeInMineshaft) {
                val timeInMineshaft = caveInTimerStart.passedSince()
                add("§fTime in mineshaft: §e${timeInMineshaft.format()}".let(Renderable::text))
            }

            if (config.showEstimatedTimeLeft) {
                val estimatedTime = estimateMaxTimeLeft()
                val estimatedTimeText = estimatedTime?.format() ?: "§7Calculating..."
                add("§fEstimated time left: §e$estimatedTimeText".let(Renderable::text))
            }
        }
    }

    private fun getColor(timeLeft: Duration): Color {
        val cautionFraction = 1.0 - config.cautionThreshold / CAVE_IN_DURATION.inPartialSeconds
        val warningFraction = 1.0 - config.warningThreshold / CAVE_IN_DURATION.inPartialSeconds
        val percentage = (1.0 - (timeLeft / CAVE_IN_DURATION)).coerceIn(0.0, 1.0)

        fun blend(from: LorenzColor, to: LorenzColor, progress: Double, speed: Double) =
            ColorUtils.blendRGB(from, to, progress.pow(1.0 / speed))

        return when {
            percentage < cautionFraction ->
                LorenzColor.GREEN.toColor()

            percentage < warningFraction -> {
                val blendRange = warningFraction - cautionFraction
                blend(LorenzColor.YELLOW, LorenzColor.RED, (percentage - cautionFraction) / blendRange, blendRange)
            }

            else -> LorenzColor.RED.toColor()
        }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, onlyOnIsland = IslandType.MINESHAFT)
    fun onGuiRenderOverlay() {
        if (display.isEmpty()) return
        config.position.renderRenderables(display, posLabel = "Mineshaft Cave-in Timer")
    }

    private fun estimateMaxTimeLeft(): Duration? {
        val current = lastColdValue ?: return null
        if (firstColdTime.isFarPast()) return null
        val elapsed = firstColdTime.passedSince().inPartialSeconds
        if (totalColdGained <= 0 || elapsed <= 0) return null
        val ratePerSecond = totalColdGained / elapsed
        return ((100 - current) / ratePerSecond).seconds
    }
}
