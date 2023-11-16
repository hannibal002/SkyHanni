package at.hannibal2.skyhanni.features.rift.everywhere.motes

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RiftMotesStats {
    val config get() = SkyHanniMod.feature.rift.motes.motesStats

    var startMotes = 0
    var latestMotes = 0
    private var display = emptyList<String>()

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return

    }

    @SubscribeEvent
    fun onRenderInventoryOverlay(event: GuiRenderEvent.ChestBackgroundRenderEvent) {
        if (!isEnabled()) return
        config.position.renderStrings(display, posLabel = "Rift Motes Stats")
    }

    @SubscribeEvent
    fun onScoreboardRawChange(event: ScoreboardRawChangeEvent) {
        if (!isEnabled()) return

    }

    @SubscribeEvent
    fun onScoreboardChange(event: ScoreboardChangeEvent) {
        if (!isEnabled()) return

    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (!isEnabled()) return

        if (event.newIsland == IslandType.THE_RIFT) {
            startMotes = -1
            latestMotes = -1
        } else if (event.oldIsland == IslandType.THE_RIFT) {
            showFinalStats()
        }
    }

    private fun showFinalStats() {

    }

    fun isEnabled() = true
}