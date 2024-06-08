package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils.hasPotionEffect
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.ColorRange
import net.minecraft.potion.Potion
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import scala.sys.process.ProcessBuilderImpl.Simple
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HealthDisplay {
    private val config get() = SkyHanniMod.feature.gui.healthDisplay

    private var health = 0.0
    private var healthLast = 0.0
    private var healthTimer = SimpleTimeMark.farPast()
    private var maxHealth = 0.0
    private var colorList = listOf<ColorRange>()

    private var healthUpdate = 0
    private var healthUpdateTimer = SimpleTimeMark.farPast()

    private var actualHealth = 0
    private var absorptionRate = 0.0
    private var hasAbsorption = false

    private enum class HealthColors(val range: ClosedFloatingPointRange<Double>, val color: Color) {
        RED(0.0..1.0, Color.RED),
        YELLOW(1.0..2.0, Color.YELLOW),
        ORANGE(2.0..3.0, Color.ORANGE),
        MAX(3.0..1000.0, Color.green) //test
        ;
        companion object {
            fun getColors(input: Double): Pair<HealthColors?, HealthColors?> {
//                 if (config.predictHealth) {
//                     return if (hasAbsorption) YELLOW to YELLOW
//                     else RED to RED
//                 }
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
        if (!LorenzUtils.inSkyBlock) return
        if (RiftAPI.inRift()) return

        maxHealth = ActionBarStatsData.MAX_HEALTH.value.replace(",", "").toDoubleOrNull() ?: return
        val hp = ActionBarStatsData.HEALTH.value.replace(",", "").toDoubleOrNull() ?: return
        val player = LorenzUtils.getPlayer() ?: return

        if (config.predictHealth) {
            if (maxHealth < hp && player.absorptionAmount != 0.0f) absorptionRate = (hp-maxHealth)/player.absorptionAmount
            return
        }
        healthUpdate = healthUpdater(hp.toInt() - actualHealth)
        actualHealth = hp.toInt()
        healthLast = health
        health = hp / maxHealth
        healthTimer = SimpleTimeMark.now()

        setColors()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!RiftAPI.inRift() && !config.predictHealth) return

        val player = LorenzUtils.getPlayer() ?: return
        hasAbsorption = player.absorptionAmount > 0.0f

        healthLast = health
        if (RiftAPI.inRift())  {
            if (maxHealth < player.maxHealth) maxHealth = player.maxHealth.toDouble()
            healthUpdate = healthUpdater(player.health.toInt() - actualHealth)
            actualHealth = player.health.toInt()
            health = actualHealth/maxHealth
            healthTimer = SimpleTimeMark.now()
        } else {
            health = (((player.health)/player.maxHealth).toDouble())
            health += (player.absorptionAmount * absorptionRate)/maxHealth
            healthTimer = SimpleTimeMark.now()
            healthUpdate = healthUpdater((health * maxHealth).toInt() - actualHealth)
            actualHealth = (health * maxHealth).toInt()
        }

        setColors()
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

    private fun setColors() {
        val colors = HealthColors.getColors(health)
        val firstColor = colors.first ?: return
        val secondColor = colors.second ?: return

        colorList = getColorList(Pair(firstColor, secondColor))
    }

    private fun healthUpdater(update: Int): Int {
        if (update == 0) {
            if (healthUpdateTimer.passedSince() > 2.seconds) {
                healthUpdateTimer = SimpleTimeMark.now()
                return 0
            } else {
                return healthUpdate
            }
        }
        healthUpdateTimer = SimpleTimeMark.now()
        return update
    }

    @SubscribeEvent
    fun onRiftEntry(event: IslandChangeEvent) {
        if (event.newIsland != IslandType.THE_RIFT) return

        val player = LorenzUtils.getPlayer() ?: return
        maxHealth = player.maxHealth.toDouble()
        ChatUtils.chat("maxHP: $maxHealth")
    }

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        if (config.enabledBar) {
            val interpolatedHealth = NumberUtil.interpolate(health.toFloat(), healthLast.toFloat(), healthTimer.toMillis()).toDouble()
            val barRenderable = Renderable.progressBarMultipleColors(
                if (interpolatedHealth > 1.0) 1.0 else interpolatedHealth,
                colorList,
            )
            config.positionBar.renderRenderables(listOf(barRenderable), posLabel = "HP Bar")
        }

        if (config.enabledText) {
            val colorCode = if (actualHealth > maxHealth) "§6"
            else "§c"

            var string = "$colorCode$actualHealth/${maxHealth.toInt()}❤"

            val updateText = when {
                healthUpdate == 0 -> ""
                healthUpdate > 0 -> "+$healthUpdate"
                else -> "$healthUpdate"
            }
            if (config.healthUpdates && updateText.isNotEmpty()) {
                string += " §c$updateText"
            }

            val textRenderable = Renderable.string(
                string
            )
            config.positionText.renderRenderables(listOf(textRenderable), posLabel = "HP Text")
        }
    }

    @SubscribeEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Health Display")

        if (!isEnabled()) {
            event.addIrrelevant("disabled in config")
            return
        }

        val player = LorenzUtils.getPlayer() ?: return
        event.addData {
            add("health: $health")
            add("healthUpdate: $healthUpdate")
            add("maxHealth: $maxHealth")

            add("absorptionRate: $absorptionRate")
            add("has absorption: ${player.hasPotionEffect(Potion.absorption)}")
            add("absroption amt: ${player.absorptionAmount}")
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && (config.enabledBar || config.enabledText)
}
