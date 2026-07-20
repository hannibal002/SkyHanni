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
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object StonksAuctionTimer {

    private val config get() = SkyHanniMod.feature.event.stonksAuction
    private val storage get() = ProfileStorageData.profileSpecific

    private var lastSeenRoundEnd = SimpleTimeMark.farPast()
    private var lastWarnedRoundEnd = SimpleTimeMark.farPast()

    private val roundEnd get() = storage?.stonksAuctionRoundEnd ?: SimpleTimeMark.farPast()
    private val hasBidThisRound get() = storage?.stonksAuctionLastBidRoundEnd == roundEnd

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.countdown) return
        if (!Perk.STOCK_EXCHANGE.isActive) return
        val roundEnd = roundEnd
        if (!roundEnd.isInFuture()) return

        val bidStatus = if (hasBidThisRound) "§a✔ Bid placed" else "§c✘ Not bid!"
        val display = Renderable.text("§6Stonks Auction §7ends in §e${roundEnd.timeUntil().format(maxUnits = 2)} $bidStatus")
        config.countdownPosition.renderRenderable(display, posLabel = "Stonks Auction Timer")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!Perk.STOCK_EXCHANGE.isActive) return
        val roundEnd = roundEnd
        if (!roundEnd.isInFuture()) return

        if (roundEnd != lastSeenRoundEnd) {
            val isFirstObservation = lastSeenRoundEnd.isFarPast()
            lastSeenRoundEnd = roundEnd
            lastWarnedRoundEnd = SimpleTimeMark.farPast()
            if (config.newRoundAlert && !isFirstObservation) {
                ChatUtils.chat("§6A new Stonks Auction round has started! §7Ends in §e${roundEnd.timeUntil().format(maxUnits = 2)}")
            }
        }

        if (config.oneMinuteWarning && roundEnd.timeUntil() <= 1.minutes && lastWarnedRoundEnd != roundEnd) {
            lastWarnedRoundEnd = roundEnd
            val bidWarning = if (hasBidThisRound) "" else " §c§lYou haven't bid yet!"
            SoundUtils.repeatSound(100, 2, SoundUtils.createSound("block.note_block.pling", 0.5f))
            TitleManager.sendTitle("§6Stonks Auction ending soon!$bidWarning")
        }
    }
}
