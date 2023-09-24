package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.getFormattedSkyblockTime
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.features.misc.discordrpc.DiscordStatus
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.concurrent.fixedRateTimer

class InGameDateDisplay {
    private val config get() = SkyHanniMod.feature.gui
    private var display = ""

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.repeatSeconds(config.inGameDateDisplayRefreshSeconds)) return

        checkDate()
    }

    private fun checkDate() {
        if (!isEnabled()) return

        display = SkyBlockTime.now().getFormattedSkyblockTime()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        config.inGameDateDisplayPosition.renderString(display, posLabel = "In-game Date Display")
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.inGameDateDisplayEnabled", "gui.inGameDateDisplay")
        event.move(2, "misc.inGameDateDisplayPosition", "gui.inGameDateDisplayPosition")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.inGameDateDisplay
}
