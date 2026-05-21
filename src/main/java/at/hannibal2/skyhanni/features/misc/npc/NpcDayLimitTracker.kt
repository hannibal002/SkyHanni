package at.hannibal2.skyhanni.features.misc.npc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.NpcDayLimitTrackerConfig.NumberFormatEntry
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@SkyHanniModule
object NpcDayLimitTracker {

    private const val DAILY_LIMIT = 500_000_000L
    private const val POS_LABEL = "NPC Day Limit Tracker"

    private val config get() = SkyHanniMod.feature.misc.npcDayLimitTracker

    private val patternGroup = RepoPattern.group("misc.npcdaylimit")

    /**
     * REGEX-TEST: You sold Evergreen Chip x64 for 9,600,000 Coins!
     * REGEX-TEST: You sold Cicada Symphony Vinyl x1 for 50,000 Coins!
     */
    private val npcSellPattern by patternGroup.pattern(
        "sold",
        "You sold .+ for (?<amount>[\\d,]+) Coins!",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.enabled) return

        for (line in event.cleanMessage.split('\n')) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            npcSellPattern.matchMatcher(trimmed) {
                val amount = group("amount").replace(",", "").toLongOrNull() ?: return@matchMatcher
                if (amount <= 0) return@matchMatcher
                rollDayIfNeeded()
                config.soldCoins += amount
            }
        }
    }

    @HandleEvent
    fun onGuiOverlayRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!shouldRenderInOverlayPass()) return
        renderHud()
    }

    @HandleEvent
    fun onGuiOnTopRender(event: GuiRenderEvent.GuiOnTopRenderEvent) {
        if (!shouldRenderOnTop()) return
        renderHud()
    }

    private fun shouldRenderInOverlayPass(): Boolean {
        if (GuiEditManager.isInGui()) return config.enabled
        if (!config.enabled || !config.showHud || !SkyBlockUtils.inSkyBlock) return false
        return !isDeferredScreenOpen()
    }

    private fun shouldRenderOnTop(): Boolean {
        if (GuiEditManager.isInGui()) return false
        if (!config.enabled || !config.showHud || !SkyBlockUtils.inSkyBlock) return false
        return isDeferredScreenOpen()
    }

    private fun isDeferredScreenOpen(): Boolean {
        val screen = Minecraft.getInstance().screen ?: return false
        return screen is InventoryScreen || screen is ChatScreen || screen is ContainerScreen
    }

    private fun renderHud() {
        config.position.renderRenderable(
            Renderable.text(hudLine()),
            posLabel = POS_LABEL,
        )
    }

    private fun rollDayIfNeeded() {
        val today = LocalDate.now(ZoneOffset.UTC).toEpochDay()
        if (config.gmtEpochDay != today) {
            config.gmtEpochDay = today
            config.soldCoins = 0L
        }
    }

    fun soldCoinsToday(): Long {
        rollDayIfNeeded()
        return config.soldCoins
    }

    fun remainingCoinsToday(): Long {
        rollDayIfNeeded()
        return (DAILY_LIMIT - config.soldCoins).coerceAtLeast(0L)
    }

    fun formatCoinsLabel(coins: Long): String = when (config.numberFormat.get()) {
        NumberFormatEntry.CONDENSED -> formatCondensed(coins)
        NumberFormatEntry.FULL -> String.format(Locale.US, "%,d", coins)
    }

    private fun formatCondensed(coins: Long): String {
        if (coins <= 0) return "0m"
        if (coins % 1_000_000 == 0L) return "${coins / 1_000_000}m"
        return String.format(Locale.ROOT, "%.1fm", coins / 1_000_000.0)
    }

    fun hudPlainText(): String {
        val sold = formatCoinsLabel(soldCoinsToday())
        val limit = formatCoinsLabel(DAILY_LIMIT)
        return "$sold/$limit"
    }

    fun hudLine(): String {
        val sold = formatCoinsLabel(soldCoinsToday())
        val limit = formatCoinsLabel(DAILY_LIMIT)
        return "§6$sold§f/§6$limit"
    }
}
