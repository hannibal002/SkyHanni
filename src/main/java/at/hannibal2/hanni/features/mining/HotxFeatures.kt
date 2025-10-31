package at.hannibal2.hanni.features.mining

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.hotx.HotfData
import at.hannibal2.hanni.data.hotx.HotmData
import at.hannibal2.hanni.data.hotx.HotxHandler
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.RenderItemTipEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.test.command.ErrorManager
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.RenderUtils.highlight
import at.hannibal2.hanni.utils.RenderUtils.renderRenderable
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.primitives.text

@HanniModule
object HotxFeatures {

    private val configHotm get() = HanniMod.feature.mining.hotm
    private val configHotf get() = HanniMod.feature.foraging.hotf

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        val (handler, configPos) = when {
            HotmData.inApplicableIsland && configHotm.skyMallDisplay -> Pair(HotmData, configHotm.skyMallPosition)
            HotfData.inApplicableIsland && configHotf.lotteryDisplay -> Pair(HotfData, configHotf.lotteryPosition)
            else -> return
        }
        val rotatingPerkEntry = handler.rotatingPerkEntry
        if (!rotatingPerkEntry.isUnlocked || !rotatingPerkEntry.enabled) return
        val currentPerk = handler.currentRotPerk

        val perkDescriptionFormat = currentPerk?.perkDescription
            ?: "§cUnknown! Run ${"§b/${handler.name.lowercase()}"} §cto fix this."
        val finalFormat = "§b${rotatingPerkEntry.guiName}§8: $perkDescriptionFormat"

        configPos.renderRenderable(
            Renderable.text(finalFormat),
            posLabel = "${rotatingPerkEntry.guiName} Display",
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: HanniChatEvent) {
        val claimMap: Map<HotxHandler<*, *, *>, Boolean?> = listOf(
            HotmData, HotfData,
        ).associateWith { data ->
            data.tryReadRotatingPerkChat(event)
        }

        val claimResults = claimMap.values
        val wasClaimed = claimResults.any { it == true }
        val noMatches = claimResults.all { it == null }
        if (wasClaimed || noMatches) return

        ErrorManager.logErrorStateWithData(
            "Could not read the rotating effect from chat",
            "no hotxhandler claimed the event",
            "chat" to event.message,
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        val handler: HotxHandler<*, *, *> = when {
            HotmData.inInventory && configHotm.highlightEnabledPerks -> HotmData
            HotfData.inInventory && configHotf.highlightEnabledPerks -> HotfData
            else -> return
        }
        handler.data.forEach { entry ->
            val color = if (!entry.isUnlocked) LorenzColor.DARK_GRAY
            else if (entry.enabled) LorenzColor.GREEN else LorenzColor.RED
            entry.slot?.highlight(color)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderTip(event: RenderItemTipEvent) {
        handleLevelStackSize(event)
        handleTokenStackSize(event)
    }

    private fun handleLevelStackSize(event: RenderItemTipEvent) {
        val handler: HotxHandler<*, *, *> = when {
            HotmData.inInventory && configHotm.levelStackSize -> HotmData
            HotfData.inInventory && configHotf.levelStackSize -> HotfData
            else -> return
        }
        handler.data.firstOrNull {
            event.stack.displayName == it.item?.displayName
        }?.let {
            event.stackTip = if (it.activeLevel == 0 || it.activeLevel == it.maxLevel) "" else "§e${it.activeLevel}"
            it.activeLevel.toString()
        }
    }

    private fun handleTokenStackSize(event: RenderItemTipEvent) {
        val handler: HotxHandler<*, *, *> = when {
            HotmData.inInventory && configHotm.tokenStackSize -> HotmData
            HotfData.inInventory && configHotf.tokenStackSize -> HotfData
            else -> return
        }
        if (event.stack.displayName != handler.heartItem?.stack?.displayName) return
        event.stackTip = handler.availableTokens.takeIf { it != 0 }?.let { "§b$it" }.orEmpty()
    }

}
