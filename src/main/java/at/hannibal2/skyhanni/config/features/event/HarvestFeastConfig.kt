package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.enums.SharePolicy
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HarvestFeastConfig {

    @Expose
    @ConfigOption(
        name = "Display current in-season crops",
        desc = "Display the current in-season Harvest Feast crops."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var displayCurrentCrops: Boolean = true

    @Expose
    @ConfigOption(
        name = "Fetch Upcoming Feast Data",
        desc = "Automatically fetch Feast Data from eliteskyblock.com for the current year if they're uploaded already.",
    )
    @ConfigEditorBoolean
    var fetchAutomatically: Boolean = true

    @Expose
    @ConfigOption(
        name = "Share Feast Data",
        desc = "Share the Harvest Feast data to eliteskyblock.com for everyone else to then fetch automatically.",
    )
    @ConfigEditorDropdown
    var sharePolicy: SharePolicy = SharePolicy.ASK

    @Expose
    @ConfigLink(owner = HarvestFeastConfig::class, field = "displayCurrentCrops")
    val position: Position = Position(400, 10)
}
