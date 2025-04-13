package at.hannibal2.skyhanni.config.features.combat

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DragonConfig {
    @Expose
    @ConfigOption(name = "Superior Notification", desc = "Notifies you with a title that a superior dragon spawned.")
    @ConfigEditorBoolean
    @FeatureToggle
    var superiorNotify: Boolean = true

    @Expose
    @ConfigOption(
        name = "Weight HUD",
        desc = "Shows your current dragon/protector weight on the HUD during the dragon/protector fight. " +
            "Hover over the HUD to show a breakdown. " +
            "The dragon widget needs to be enabled for this to work.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var display: Boolean = false

    @Expose
    @ConfigLink(owner = DragonConfig::class, field = "display")
    var displayPosition: Position = Position(120, 40, false, true)

    @Expose
    @ConfigOption(name = "Weight Message", desc = "Shows your dragon weight in chat after the dragon died.")
    @ConfigEditorBoolean
    @FeatureToggle
    var chat: Boolean = true
}
