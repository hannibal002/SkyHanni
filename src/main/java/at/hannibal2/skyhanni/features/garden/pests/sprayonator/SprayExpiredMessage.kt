package at.hannibal2.skyhanni.features.garden.pests.sprayonator

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.createCommaSeparatedList
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting

@SkyHanniModule
object SprayExpiredMessage {
    private val config get() = PestApi.config.spray

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onSecondPassed() {
        if (config.expiryNotification) {
            sendExpiredPlotsToChat(false)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onIslandJoin() {
        if (!config.expiryNotification) return
        sendExpiredPlotsToChat(true)
    }

    private fun sendExpiredPlotsToChat(wasAway: Boolean) {
        val expiredPlots = GardenPlotApi.plots.filter { it.isSprayExpired }
        if (expiredPlots.isEmpty()) return

        expiredPlots.forEach { it.markExpiredSprayAsNotified() }
        val expiredSprayMessages = componentBuilder {
            val wasAwayString = if (wasAway) "While you were away, your" else "Your"
            appendWithColor(wasAwayString, ChatFormatting.GRAY)
            val sprayString = "spray".pluralize(expiredPlots.size)
            appendWithColor(" $sprayString on ", ChatFormatting.GRAY)
            appendWithColor("Plot", ChatFormatting.GREEN)
            appendWithColor(" - ", ChatFormatting.GRAY)
            val plotsComponent = expiredPlots.map { it.name.asComponent().withColor(ChatFormatting.AQUA) }
                .createCommaSeparatedList(ChatFormatting.GRAY)

            append(plotsComponent)
            appendWithColor(" expired.", ChatFormatting.GRAY)
        }
        ChatUtils.chat(expiredSprayMessages)
    }
}
