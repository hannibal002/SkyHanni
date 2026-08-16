package at.hannibal2.skyhanni.features.garden.pests.sprayonator

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.garden.GardenPlotSprayDataTablistReadEvent
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.bold
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object SprayAddedMessage {
    private val config get() = PestApi.config.spray

    @HandleEvent
    private fun onGardenSprayTablistUpdate(event: GardenPlotSprayDataTablistReadEvent) {
        val currentSpray = event.currentSpray
        val newSpray = event.newSpray
        val expiryTime = event.newSprayExpiryTime
        if (currentSpray == null) {
            if (config.newSprayNotification) {
                sendSprayMessage(event)
            }
            return
        }
        if (sprayMessageEligible(currentSpray.expiry, expiryTime, event.newSpray, newSpray)) {
            sendSprayMessage(event)
        }
    }

    @Suppress("HandleEventInspection")
    private fun sendSprayMessage(event: GardenPlotSprayDataTablistReadEvent) {
        val time = event.newSprayExpiryTime.timeUntil().format()
        ChatUtils.chat(buildFirstSprayMessage(event.plotName, event.newSpray.name))
        ChatUtils.chat(buildSecondSprayMessage(time))
    }

    private fun buildFirstSprayMessage(plot: String, spray: String): Component = componentBuilder {
        appendWithColor("Plot ", ChatFormatting.GREEN)
        appendWithColor("- ", ChatFormatting.GRAY)
        appendWithColor("$plot ", ChatFormatting.AQUA)
        appendWithColor("was sprayed with ", ChatFormatting.GRAY)
        appendWithColor(spray, ChatFormatting.GREEN)
        appendWithColor("!", ChatFormatting.GRAY)
    }

    private fun buildSecondSprayMessage(time: String): Component = componentBuilder {
        appendWithColor("This will expire in", ChatFormatting.GRAY)
        appendWithColor(time, ChatFormatting.GREEN)
        appendWithColor("!", ChatFormatting.GRAY)
    }

    private fun sprayMessageEligible(
        sprayExpiryTime: SimpleTimeMark, expectedExpireTime: SimpleTimeMark, currentSpray: SprayType, newSpray: SprayType,
    ): Boolean {
        return (sprayExpiryTime <= expectedExpireTime - 10.minutes || currentSpray != newSpray) &&
            (config.newSprayNotification && sprayExpiryTime >= SimpleTimeMark.now() + 1.minutes)
    }
}
