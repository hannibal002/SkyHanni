package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

// TODO CONFIG FIX; DOESN'T CURRENTLY SAVE
class EliteFarmersLeaderboardsConfig {
    @Expose
    @ConfigOption(name = "Farming Weight Display", desc = "")
    @Accordion
    val farmingWeightDisplay: FarmingWeightDisplayConfig = FarmingWeightDisplayConfig()

    @Expose
    @ConfigOption(name = "Crop Collection Display", desc = "")
    @Accordion
    val cropCollectionDisplay: CropCollectionDisplayConfig = CropCollectionDisplayConfig()

    @Expose
    @ConfigOption(name = "Pest Kills Display", desc = "")
    @Accordion
    val pestKillsDisplay: PestKillsDisplayConfig = PestKillsDisplayConfig()


}
