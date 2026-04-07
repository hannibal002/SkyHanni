package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.foraging.HotfConfig.LotteryDisplayVisibility
import at.hannibal2.skyhanni.config.features.mining.HotmConfig.SkyMallDisplayVisibility
import at.hannibal2.skyhanni.data.hotx.HotfData
import at.hannibal2.skyhanni.data.hotx.HotmData
import at.hannibal2.skyhanni.data.hotx.HotxHandler
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object HotxFeatures {

    private val configHotm get() = SkyHanniMod.feature.mining.hotm
    private val configHotf get() = SkyHanniMod.feature.foraging.hotf

    private val handlers = listOf(HotmData, HotfData)

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, onlyOnSkyblock = true)
    fun onGuiRenderOverlay() {
        handlers.forEach { it.renderOverlay() }
    }

    private fun HotxHandler<*, *, *>.renderOverlay() {
        if (!shouldShowDisplay) return
        val rotatingPerkEntry = rotatingPerkEntry
        if (!rotatingPerkEntry.isUnlocked || !rotatingPerkEntry.enabled) return
        val currentPerk = currentRotPerk

        val perkDescriptionFormat = currentPerk?.perkDescription
            ?: "§cUnknown! Run ${"§b/${name.lowercase()}"} §cto fix this."
        val finalFormat = "§b${rotatingPerkEntry.guiName}§8: $perkDescriptionFormat"

        position.renderRenderable(
            Renderable.text(finalFormat),
            posLabel = "${rotatingPerkEntry.guiName} Display",
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
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
            "no HotxHandler claimed the event",
            "chat" to event.cleanMessage,
        )
    }

    @HandleEvent(GuiContainerEvent.BackgroundDrawnEvent::class, onlyOnSkyblock = true)
    fun onBackgroundDrawn() {
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
            event.stack.hoverName.string == it.item?.hoverName?.string
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
        if (event.stack.hoverName.string != handler.heartItem?.item?.hoverName?.string) return
        event.stackTip = handler.availableTokens.takeIf { it != 0 }?.let { "§b$it" }.orEmpty()
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(125, "mining.hotm.skyMallDisplay") {
            ConfigUtils.migrateBooleanToEnum(it, SkyMallDisplayVisibility.MINING_ONLY, SkyMallDisplayVisibility.OFF)
        }
        event.transform(125, "foraging.hotf.lotteryDisplay") {
            ConfigUtils.migrateBooleanToEnum(it, LotteryDisplayVisibility.FORAGING_ONLY, LotteryDisplayVisibility.OFF)
        }
    }
}
