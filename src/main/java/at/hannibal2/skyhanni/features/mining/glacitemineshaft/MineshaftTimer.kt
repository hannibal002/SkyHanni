package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.events.mining.GlaciteMineshaftDetectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MineshaftCaveInTimer {

    private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft.mineshaftTimerConfig

    private val CAVE_IN_DURATION = 60.seconds

    private var caveInTimerStart = SimpleTimeMark.farPast()

    private var display: List<Renderable> = emptyList()

    @HandleEvent(WorldChangeEvent::class)
    fun onWorldChange() {
        caveInTimerStart = SimpleTimeMark.farPast()
        display = emptyList()
    }

    @HandleEvent
    fun onMineshaftDetect(event: GlaciteMineshaftDetectEvent) {
        caveInTimerStart = SimpleTimeMark.now()
    }

    @HandleEvent(SecondPassedEvent::class, onlyOnIsland = IslandType.MINESHAFT)
    fun onSecondPassed() {
        if (!config.enabled || caveInTimerStart.isFarPast()) {
            display = emptyList()
            return
        }

        val timeLeft = CAVE_IN_DURATION - caveInTimerStart.passedSince()

        val warningThreshold = config.warningThreshold.seconds
        val cautionThreshold = config.cautionThreshold.seconds

        val caveInColor = when {
            timeLeft <= warningThreshold -> LorenzColor.RED.getChatColor()
            timeLeft <= cautionThreshold -> LorenzColor.YELLOW.getChatColor()
            else -> LorenzColor.GREEN.getChatColor()
        }
        val caveInText = if (timeLeft.isNegative()) "Caved in!" else timeLeft.format()

        display = buildList {
            add("§fEntrance caves in: $caveInColor$caveInText".let(Renderable::text))

            if (config.showTimeInMineshaft) {
                val timeInMineshaft = caveInTimerStart.passedSince()
                add("§fTime in mineshaft: §e${timeInMineshaft.format()}".let(Renderable::text))
            }
        }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, onlyOnIsland = IslandType.MINESHAFT)
    fun onRenderOverlay() {
        if (display.isEmpty()) return
        config.position.renderRenderables(display, posLabel = "Mineshaft Cave-in Timer")
    }
}
