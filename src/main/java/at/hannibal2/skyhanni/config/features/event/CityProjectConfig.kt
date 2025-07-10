package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CityProjectConfig {
    @Expose
    @ConfigOption(name = "Show Materials", desc = "Show materials needed for contributing to the City Project.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showMaterials: Boolean = true

    @Expose
    @ConfigOption(name = "Show Ready", desc = "Mark contributions that are ready to participate.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showReady: Boolean = true

    @Expose
    @ConfigOption(name = "Daily Reminder", desc = "Remind every 24 hours to participate.")
    @ConfigEditorBoolean
    @FeatureToggle
    var dailyReminder: Boolean = true

    @Expose
    @ConfigLink(owner = CityProjectConfig::class, field = "showMaterials")
    val pos: Position = Position(150, 150)
}
