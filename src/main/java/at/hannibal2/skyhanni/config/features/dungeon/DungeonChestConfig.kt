package at.hannibal2.skyhanni.config.features.dungeon

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DungeonChestConfig {
    @Expose
    @ConfigOption(
        name = "Show Used Kismet",
        desc = "Add a visual highlight for used Kismet Feathers to the Croesus inventory.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showUsedKismets: Boolean = true

    @Expose
    @ConfigOption(name = "Kismet Amount", desc = "Show the amount of Kismet Feathers as stack size.")
    @ConfigEditorBoolean
    @FeatureToggle
    var kismetStackSize: Boolean = true

    @Expose
    @ConfigOption(
        name = "Croesus Limit Warning",
        desc = "Give a warning when you are close to being past Croesus limit.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var croesusLimit: Boolean = true

    @Expose
    @ConfigOption(
        name = "Croesus Unopened Overlay",
        desc = "Displays current number of Unopened Croesus chests out of 60.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var croesusOverlay: Boolean = false

    @Expose
    @ConfigOption(
        name = "Show In Kuudra",
        desc = "Displays current number of Unopened Croesus chests out of 60 in Kuudra.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var croesusOverlayKuudra: Boolean = false

    @Expose
    @ConfigOption(
        name = "Show In Dungeons",
        desc = "Displays current number of Unopened Croesus chests out of 60 in Dungeons.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var croesusOverlayDungeons: Boolean = false

    @Expose
    @ConfigLink(owner = DungeonChestConfig::class, field = "croesusOverlay")
    val croesusOverlayPosition: Position = Position(200, 100)
}
