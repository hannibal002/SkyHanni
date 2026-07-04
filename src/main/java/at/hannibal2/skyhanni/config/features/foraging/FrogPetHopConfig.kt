package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FrogPetHopConfig {

    @Expose
    @ConfigOption(
        name = "Hop Timer",
        desc = "Show a countdown for the §aFrog Pet§7's §6Hop§7 ability (§5Epic§7+).\n" +
            "Jump to gain §2Foraging Fortune§7 for 20s.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Warning Threshold", desc = "Warn this many seconds before the §6Hop§7 buff expires.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var warningThreshold: Int = 4

    @Expose
    @ConfigOption(name = "Warning Sound", desc = "Play a sound when the §6Hop§7 buff is about to expire or has expired.")
    @ConfigEditorBoolean
    var warningSound: Boolean = true

    @Expose
    @ConfigOption(name = "Warning Title", desc = "Show a title warning when the §6Hop§7 buff is about to expire.")
    @ConfigEditorBoolean
    var warningTitle: Boolean = true

    @Expose
    @ConfigOption(name = "Warning Chat", desc = "Send a chat message when the §6Hop§7 buff is about to expire.")
    @ConfigEditorBoolean
    var warningChat: Boolean = true

    @Expose
    @ConfigOption(name = "Expired Chat", desc = "Send a chat message when the §6Hop§7 buff has expired.")
    @ConfigEditorBoolean
    var expiredChat: Boolean = true

    @Expose
    @ConfigLink(owner = FrogPetHopConfig::class, field = "enabled")
    val position: Position = Position(-372, 53)
}
