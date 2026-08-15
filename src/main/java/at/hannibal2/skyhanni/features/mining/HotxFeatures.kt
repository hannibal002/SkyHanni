package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.foraging.HotfConfig.RotatingPerkDisplayVisibility
import at.hannibal2.skyhanni.config.features.mining.HotmConfig.SkyMallDisplayVisibility
import at.hannibal2.skyhanni.data.hotx.HotfData
import at.hannibal2.skyhanni.data.hotx.HotmData
import at.hannibal2.skyhanni.data.hotx.HotxHandler
import at.hannibal2.skyhanni.data.hotx.RotatingPerkSlot
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object HotxFeatures {

    private val configHotm get() = SkyHanniMod.feature.mining.hotm
    private val configHotf get() = SkyHanniMod.feature.foraging.hotf

    private val handlers = listOf(HotmData, HotfData)

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, onlyOnSkyblock = true)
    private fun onGuiRenderOverlay() {
        handlers.forEach { it.renderOverlay() }
    }

    private fun HotxHandler<*, *>.renderOverlay() {
        if (!shouldShowDisplay) return
        val renderables = rotatingPerkSlots.mapNotNull { it.displayLine(name) }.map { Renderable.text(it) }
        if (renderables.isEmpty()) return

        position.renderRenderables(
            renderables,
            posLabel = "$name Display",
        )
    }

    private fun RotatingPerkSlot<*, *>.displayLine(treeName: String): String? {
        if (!entry.isUnlocked || !entry.enabled) return null
        val description = currentPerk?.displayDescription
            ?: "§cUnknown! Run §b/${treeName.lowercase()} §cto fix this."
        return "§b${entry.guiName}§8: $description"
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val claimResults = handlers.map { it.tryReadRotatingPerkChat(event) }
        val wasClaimed = claimResults.any { it == true }
        val noMatches = claimResults.all { it == null }
        if (wasClaimed || noMatches) return

        ErrorManager.logErrorStateWithData(
            "Could not read the rotating effect from chat",
            "no HotxHandler claimed the event",
            "chat" to event.cleanMessage,
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onBackgroundDrawn() {
        val handler: HotxHandler<*, *> = when {
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
    private fun onRenderTip(event: RenderItemTipEvent) {
        event.handleLevelStackSize()
        event.handleTokenStackSize()
    }

    private fun RenderItemTipEvent.handleLevelStackSize() {
        val handler: HotxHandler<*, *> = when {
            HotmData.inInventory && configHotm.levelStackSize -> HotmData
            HotfData.inInventory && configHotf.levelStackSize -> HotfData
            else -> return
        }
        handler.data.firstOrNull {
            stack.hoverName.string == it.item?.hoverName?.string
        }?.let {
            stackTip = if (it.activeLevel == 0 || it.activeLevel == it.maxLevel) "" else "§e${it.activeLevel}"
            it.activeLevel.toString()
        }
    }

    private fun RenderItemTipEvent.handleTokenStackSize() {
        val handler: HotxHandler<*, *> = when {
            HotmData.inInventory && configHotm.tokenStackSize -> HotmData
            HotfData.inInventory && configHotf.tokenStackSize -> HotfData
            else -> return
        }
        if (stack.hoverName.string != handler.heartItem?.item?.hoverName?.string) return
        stackTip = handler.availableTokens.takeIf { it != 0 }?.let { "§b$it" }.orEmpty()
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(125, "mining.hotm.skyMallDisplay") {
            ConfigUtils.migrateBooleanToEnum(it, SkyMallDisplayVisibility.MINING_ONLY, SkyMallDisplayVisibility.OFF)
        }
        event.transform(125, "foraging.hotf.lotteryDisplay") {
            ConfigUtils.migrateBooleanToEnum(
                it,
                RotatingPerkDisplayVisibility.FORAGING_ONLY,
                RotatingPerkDisplayVisibility.OFF,
            )
        }
    }
}
