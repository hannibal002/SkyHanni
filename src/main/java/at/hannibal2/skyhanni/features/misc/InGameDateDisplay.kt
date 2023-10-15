package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.formatted
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class InGameDateDisplay {
    private val config get() = SkyHanniMod.feature.gui.inGameDateConfig
    private var display = ""

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!LorenzUtils.inSkyBlock) return
        if (!config.useScoreboard && !event.repeatSeconds(config.refreshSeconds)) return
        if (config.useScoreboard && !event.repeatSeconds(1)) return

        checkDate()
    }

    private fun checkDate() {
        val date = SkyBlockTime.now()
        var theBaseString = ""
        if (config.useScoreboard) {
            val list = ScoreboardData.sidebarLinesFormatted //we need this to grab the moon/sun symbol
            val year = "Year ${date.year}"
            val monthAndDate = list.find{ (it.contains("Winter ") || it.contains("Spring ") || it.contains("Summer ") || it.contains("Autumn ")) && (it.contains("st") || it.contains("nd") || it.contains("rd") || it.contains("th")) } ?: "??"
            val time = list.find{ it.lowercase().contains("am ") || it.lowercase().contains("pm ") } ?: "??"
            theBaseString = "$monthAndDate, $year ${time.trim()}".removeColor()
            if (!config.includeSunMoon) theBaseString = theBaseString.replace("☽", "").replace("☀", "")
        } else {
            theBaseString = date.formatted()
            if (config.includeSunMoon) {
                if ((date.hour >= 6) && (date.hour < 17)) theBaseString = "$theBaseString ☀"
                else theBaseString = "$theBaseString ☽"
            }
        }
        display = theBaseString
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.position.renderString(display, posLabel = "In-game Date Display")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
