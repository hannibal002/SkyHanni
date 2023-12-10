package at.hannibal2.skyhanni.features.event.winter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.RenderUtils.addItemIcon
import at.hannibal2.skyhanni.utils.RenderUtils.renderSingleLineWithItems
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class JyrreTimer {
    private val config get() = SkyHanniMod.feature.event.winter.jyrreTimer
    private val drankBottlePattern by RepoPattern.pattern(
        "event.winter.drank.jyrre",
        "§aYou drank a §r§6Refined Bottle of Jyrre §r§aand gained §r§b\\+300✎ Intelligence §r§afor §r§b60 minutes§r§a!"
    )
    private var display = emptyList<Any>()
    private var duration = 0.seconds

    init {
        fixedRateTimer(name = "skyhanni-update-jyrre-display", period = 1000L) {
            try {
                updateJyrreDisplay()
            } catch (error: Throwable) {
                ErrorManager.logErrorWithData(error, "Error Updating Jyrre Timer")
            }
        }
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        resetDisplay()
    }

    private fun resetDisplay() {
        display = if (config.showInactive) drawDisplay() else emptyList()
        duration = 0.seconds
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled() || !drankBottlePattern.matches(event.message)) return
        duration = 60.minutes
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.pos.renderSingleLineWithItems(display, posLabel = "Refined Jyrre Timer")
    }

    private fun updateJyrreDisplay() {
        if (!isEnabled()) return

        if (display.isNotEmpty() && !config.showInactive && duration <= 0.seconds) {
            resetDisplay()
            return
        }

        display = drawDisplay()
    }

    private val displayIcon by lazy { "REFINED_BOTTLE_OF_JYRRE".asInternalName().getItemStack() }

    fun drawDisplay(): MutableList<Any> {
        duration -= 1.seconds

        return mutableListOf<Any>().apply {
            addItemIcon(displayIcon)
            add("§aJyrre Boost: ")

            if (duration <= 0.seconds && config.showInactive) {
                add("§cInactive!")
            } else {
                val format = TimeUtils.formatDuration(duration)
                add("§b$format")
            }
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
