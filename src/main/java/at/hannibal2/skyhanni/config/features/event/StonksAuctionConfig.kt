package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class StonksAuctionConfig {
    @Expose
    @ConfigOption(name = "Countdown", desc = "Display a countdown until the current Stonks Auction round ends.")
    @ConfigEditorBoolean
    @FeatureToggle
    var countdown: Boolean = true

    @Expose
    @ConfigOption(
        name = "One Minute Warning",
        desc = "Warn you when the Stonks Auction round has less than a minute left, especially if you haven't bid yet.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var oneMinuteWarning: Boolean = true

    @Expose
    @ConfigOption(name = "New Round Alert", desc = "Send a chat message when a new Stonks Auction round starts.")
    @ConfigEditorBoolean
    @FeatureToggle
    var newRoundAlert: Boolean = true

    @Expose
    @ConfigLink(owner = StonksAuctionConfig::class, field = "countdown")
    val countdownPosition: Position = Position(-10, 10)
}
