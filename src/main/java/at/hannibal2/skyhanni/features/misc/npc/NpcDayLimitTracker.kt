package at.hannibal2.skyhanni.features.misc.npc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.NpcDayLimitTrackerConfig.NumberFormatEntry
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.time.LocalDate
import java.time.ZoneOffset

@SkyHanniModule
object NpcDayLimitTracker {

    private const val DAILY_LIMIT = 500_000_000L
    private const val POS_LABEL = "NPC Day Limit Tracker"

    private val config get() = SkyHanniMod.feature.misc.npcDayLimitTracker
    private val storage get() = ProfileStorageData.profileSpecific?.npcDayLimit

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

        npcSellPattern.matchMatcher(event.cleanMessage) {
            val amount = group("amount").formatLong()
            if (amount <= 0) return@matchMatcher
            rollDayIfNeeded()
            soldCoins += amount
        }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, onlyOnSkyblock = true)
    fun onGuiRenderOverlay() {
        if (!config.enabled) return

        config.position.renderRenderable(
            Renderable.text(hudLine()),
            posLabel = POS_LABEL,
        )
    }

    private var gmtEpochDay: Long
        get() = storage?.gmtEpochDay ?: 0L
        set(value) {
            storage?.gmtEpochDay = value
        }

    private var soldCoins: Long
        get() = storage?.soldCoins ?: 0L
        set(value) {
            storage?.soldCoins = value
        }

    private fun rollDayIfNeeded() {
        val today = LocalDate.now(ZoneOffset.UTC).toEpochDay()
        if (gmtEpochDay != today) {
            gmtEpochDay = today
            soldCoins = 0L
        }
    }

    fun soldCoinsToday(): Long {
        rollDayIfNeeded()
        return soldCoins
    }

    fun remainingCoinsToday(): Long {
        rollDayIfNeeded()
        return (DAILY_LIMIT - soldCoins).coerceAtLeast(0L)
    }

    fun formatCoinsLabel(coins: Long): String = when (config.numberFormat.get()) {
        NumberFormatEntry.SHORT -> coins.shortFormat()
        NumberFormatEntry.LONG -> coins.addSeparators()
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
