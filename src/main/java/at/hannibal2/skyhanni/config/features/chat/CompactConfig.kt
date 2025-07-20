package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CompactConfig {
    @Expose
    @ConfigOption(name = "Compact Potion Messages", desc = "")
    @Accordion
    val compactPotionMessages: CompactPotionConfig = CompactPotionConfig()

    @Expose
    @ConfigOption(
        name = "Compact Bestiary Messages",
        desc = "Compact the Bestiary level up message, only showing additional information when hovering.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var compactBestiaryMessage: Boolean = true

    @Expose
    @ConfigOption(
        name = "Compact Enchanting Rewards",
        desc = "Compact the rewards gained from Add-ons and Experiments in Experimentation Table,\n" +
            "only showing additional information when hovering.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var compactExperimentationTable: Boolean = false

    @Expose
    @ConfigOption(
        name = "Compact Jacob Claim",
        desc = "Compact the Jacob Claim message, only showing full information when hovering."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var compactJacobClaim: Boolean = false
}
