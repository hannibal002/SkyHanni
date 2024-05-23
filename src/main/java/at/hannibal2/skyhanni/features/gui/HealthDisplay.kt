package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.ColorRange
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class HealthDisplay {
    private val config get() = SkyHanniMod.feature.gui.healthDisplay

    private var health = 0.0
    private var colorList = listOf<ColorRange>()

    private enum class HealthColors(val range: ClosedFloatingPointRange<Double>, val color: Color) {
        RED(0.0..1.0, Color.RED),
        YELLOW(1.0..2.0, Color.YELLOW),
        ORANGE(2.0..3.0, Color.ORANGE),
        MAX(3.0..1000.0, Color.green) //test
        ;
        companion object {
            fun getColors(input: Double): Pair<HealthColors?, HealthColors?> {
                val color1 = entries.firstOrNull { input in it.range }
                val color2 = if (color1 == RED) {
                    RED
                } else entries.firstOrNull { (input - 1.0) in it.range }
                return color1 to color2
            }
        }
    }

    @SubscribeEvent
    fun onActionBar(event: ActionBarUpdateEvent) {
        val hp = ActionBarStatsData.HEALTH.value.replace(",", ".").toDoubleOrNull() ?: return
        val maxHP = ActionBarStatsData.MAX_HEALTH.value.replace(",", ".").toDoubleOrNull() ?: return

        health = hp / maxHP
        val colors = HealthColors.getColors(health)

        val firstColor = colors.first ?: return
        val secondColor = colors.second ?: return

        colorList = getColorList(Pair(firstColor, secondColor))
    }

    private fun getColorList(input: Pair<HealthColors, HealthColors>): List<ColorRange> {
        if (input.first == HealthColors.RED) return listOf(ColorRange(0.0, 1.0, Color.RED))

        var newHealth = health
        while (newHealth > 1.0) {
            newHealth -= 1.0
        }

        return listOf(
            ColorRange(0.0, newHealth, input.first.color),
            ColorRange(newHealth, 1.0, input.second.color)
        )
    }

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        val barRenderable = Renderable.progressBarMultipleColors(
            if (health > 1.0) 1.0 else health,
            colorList,
        )

        config.positionBar.renderRenderables(listOf(barRenderable), posLabel = "HP Bar")
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabledBar
}
