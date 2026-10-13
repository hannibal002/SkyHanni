package at.hannibal2.skyhanni.features.event.stockexchange

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.timerColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object StonksAuctionTimer {

    private val config get() = SkyHanniMod.feature.event.stonksAuction
    private val storage get() = ProfileStorageData.profileSpecific

    // real rounds are at least 1h40m apart; anything smaller is just re-parse jitter from re-reading the same round's tooltip
    private val ROUND_CHANGE_TOLERANCE = 5.minutes

    private var lastSeenRoundEnd = SimpleTimeMark.farPast()
    private var lastWarnedRoundEnd = SimpleTimeMark.farPast()
    private var display: Renderable? = null

    // rounds are anchored to SkyBlock calendar month boundaries (1 SkyBlock month = 10h20m = the observed round length),
    // used as an estimate until the exact value has been read from the in-game tooltip at least once this round
    private val roundEnd: SimpleTimeMark
        get() {
            val exact = storage?.stonksAuctionRoundEnd
            if (exact != null && exact.isInFuture()) return exact
            return calendarEstimatedRoundEnd()
        }

    private val hasBidThisRound get() = storage?.stonksAuctionLastBidRoundEnd == roundEnd
    private val bidAmount get() = storage?.stonksAuctionBidAmount ?: 0L

    private fun isSameRound(a: SimpleTimeMark, b: SimpleTimeMark) = a.absoluteDifference(b) <= ROUND_CHANGE_TOLERANCE

    private fun calendarEstimatedRoundEnd(): SimpleTimeMark {
        val now = SkyBlockTime.now()
        val monthStart = SkyBlockTime(year = now.year, month = now.month)
        return (monthStart + SkyBlockTime.SKYBLOCK_MONTH_MILLIS.milliseconds).toTimeMark()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.countdown) return
        display?.let { config.countdownPosition.renderRenderable(it, posLabel = "Stonks Auction Timer") }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!Perk.STOCK_EXCHANGE.isActive) {
            display = null
            return
        }
        val roundEnd = roundEnd
        if (!roundEnd.isInFuture()) {
            display = null
            return
        }

        updateNewRoundState(roundEnd)
        updateOneMinuteWarning(roundEnd)

        display = if (config.countdown) buildDisplay(roundEnd) else null
    }

    private fun buildDisplay(roundEnd: SimpleTimeMark): Renderable {
        val timeUntil = roundEnd.timeUntil()
        val color = timeUntil.timerColor("§e")
        val bidStatus = if (hasBidThisRound) "§a✔ Bid: §e${bidAmount.addSeparators()} Coins" else "§c✘ Not bid!"
        val countdownLine = Renderable.text("§6Stonks Auction §7ends in $color${timeUntil.format(maxUnits = 2)}")
        val bidLine = Renderable.text(bidStatus)
        val textRenderable = Renderable.vertical(countdownLine, bidLine, horizontalAlign = HorizontalAlignment.CENTER)
        return Renderable.drawInsideRoundedRect(textRenderable, color = Color(255, 255, 255, 100), radius = 5)
    }

    private fun updateNewRoundState(roundEnd: SimpleTimeMark) {
        if (isSameRound(roundEnd, lastSeenRoundEnd)) return
        val isFirstObservation = lastSeenRoundEnd.isFarPast()
        lastSeenRoundEnd = roundEnd
        lastWarnedRoundEnd = SimpleTimeMark.farPast()
        if (config.newRoundAlert && !isFirstObservation) {
            ChatUtils.chat("§6A new Stonks Auction round has started! §7Ends in §e${roundEnd.timeUntil().format(maxUnits = 2)}")
        }
    }

    private fun updateOneMinuteWarning(roundEnd: SimpleTimeMark) {
        if (!config.oneMinuteWarning) return
        if (roundEnd.timeUntil() > 1.minutes) return
        if (isSameRound(roundEnd, lastWarnedRoundEnd)) return

        lastWarnedRoundEnd = roundEnd
        val bidWarning = if (hasBidThisRound) "" else " §c§lYou haven't bid yet!"
        SoundUtils.repeatSound(100, 2, SoundUtils.createSound("block.note_block.pling", 0.5f))
        TitleManager.sendTitle("§6Stonks Auction ending soon!$bidWarning")
    }
}
