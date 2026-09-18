package at.hannibal2.skyhanni.features.event.winter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AutoUpdatingItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object JyrreTimer {

    private val config get() = SkyHanniMod.feature.event.winter.jyrreTimer

    /**
     * REGEX-TEST: You consumed a Refined Bottle of Jyrre and gained +300 Intelligence for 60m!
     */
    @Suppress("MaxLineLength")
    private val drankBottlePattern by RepoPattern.pattern(
        "event.winter.drank.jyrre.colorless",
        "You consumed a Refined Bottle of Jyrre and gained \\+300${SkyblockStat.INTELLIGENCE.hypixelIcon} Intelligence for 60m!",
    )
    private var display: Renderable? = null
    private var duration = 0.seconds

    @HandleEvent
    private fun onProfileJoin() {
        resetDisplay()
    }

    private fun resetDisplay() {
        if (display == null) return
        display = if (config.showInactive) drawDisplay() else null
        duration = 0.seconds
    }

    @HandleEvent
    private fun onSystemMessage(event: SystemMessageEvent.Allow) {
        if (!isEnabled() || !drankBottlePattern.matches(event.cleanMessage)) return
        duration = 60.minutes
    }

    @HandleEvent
    private fun onGuiRenderOverlay() {
        if (!isEnabled()) return
        display?.let {
            config.pos.renderRenderable(it, posLabel = "Refined Jyrre Timer")
        }
    }

    @HandleEvent
    private fun onSecondPassed() {
        if (!isEnabled()) return

        if (display != null && !config.showInactive && duration <= 0.seconds) {
            resetDisplay()
            return
        }

        display = drawDisplay()
    }

    private val displayIcon by AutoUpdatingItemStack("REFINED_BOTTLE_OF_JYRRE")

    fun drawDisplay(): Renderable {
        duration -= 1.seconds

        return Renderable.horizontal {
            addItemStack(displayIcon)
            addString("§aJyrre Boost: ")

            if (duration <= 0.seconds && config.showInactive) {
                addString("§cInactive!")
            } else {
                addString("§b${duration.format()}")
            }
        }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
