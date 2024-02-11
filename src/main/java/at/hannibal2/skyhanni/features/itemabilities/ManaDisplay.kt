package at.hannibal2.skyhanni.features.itemabilities

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ManaAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ManaChangeEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object ManaDisplay {
    private val config get() = SkyHanniMod.feature.misc.playerStatsDisplay

    var display = listOf<String>()

    @SubscribeEvent
    fun onManaChange(event: ManaChangeEvent) {
        display = buildList {
            add(firstLine())
            add(getSecondLine())
        }
    }

    private fun firstLine(): String {
        val estimatedMana = ManaAPI.estimatedMana
        val maxMana = ManaAPI.maxMana
        if (maxMana == -1.0) {
            return "§bLoading mana.."
        }

        return "§b${estimatedMana.addSeparators()}/${maxMana.addSeparators()}"
    }

    private fun getSecondLine(): String {
        val estimatedMana = ManaAPI.estimatedMana
        val maxMana = ManaAPI.maxMana
        val secondLine = if (ManaAPI.lastAbilityCastTime.passedSince() < 1.5.seconds) {
            ManaAPI.lastAbilityCastText
        } else {
            if (estimatedMana == maxMana) {
                ""
            } else {
                val regenPerTick = ManaAPI.regenPerTick
                "§a+ ${regenPerTick.addSeparators()}"
            }
        }
        return secondLine
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (!config.mana) return

        config.manaPosition.renderStrings(display, posLabel = "Mana Display")
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
    }
}
