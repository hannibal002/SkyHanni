package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyModern
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ForagingTrackerConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Track drops from foraging.")
    @ConfigEditorBoolean
    @OnlyModern
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = ForagingTrackerConfig::class, field = "enabled")
    val position: Position = Position(-300, 300)

    @Expose
    @ConfigOption(name = "Compact Gifts", desc = "Compact the chat messages when you receive a tree gift.")
    @ConfigEditorBoolean
    @OnlyModern
    var compactGiftChats: Boolean = true

    @Expose
    @ConfigOption(name = "Only Holding Axe", desc = "Only show the tracker while holding an axe.")
    @ConfigEditorBoolean
    @OnlyModern
    var onlyHoldingAxe: Boolean = true

    @Expose
    @ConfigOption(
        name = "Disappearing Delay",
        desc = "The delay in seconds before the tracker disappears after you stop holding an axe."
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 60f, minStep = 1f)
    @OnlyModern
    var disappearingDelay: Int = 15

    @Expose
    @ConfigOption(name = "Show Whole Trees", desc = "Estimate how many full trees you have chopped down, using percentage summing.")
    @ConfigEditorBoolean
    @OnlyModern
    var showWholeTrees: Boolean = true

}
