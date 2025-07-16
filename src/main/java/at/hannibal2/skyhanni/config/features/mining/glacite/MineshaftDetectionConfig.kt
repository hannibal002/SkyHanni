package at.hannibal2.skyhanni.config.features.mining.glacite

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftDetection
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MineshaftDetectionConfig {
    @Expose
    @ConfigOption(
        name = "Mineshaft Detection",
        desc = "Detects when you enter a mineshaft and displays the type of mineshaft you entered."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var mineshaftDetection: Boolean = true

    @Expose
    @ConfigOption(
        name = "Send Type to Party Chat",
        desc = "Sends the type of mineshaft you entered to party chat."
    )
    @ConfigEditorBoolean
    var sendTypeToPartyChat: Boolean = true

    @Expose
    @ConfigOption(
        name = "Party Chat Format",
        desc = "The party chat message format.\n" +
            "Available variables: §e{type}§7, §e{amountSinceThis}§7, §e{timeSinceThis}\n" +
            "§eNote: Using multiple variables can cause the message to be too long and be cut off."
    )
    @ConfigEditorText
    var partyChatFormat: String = "Entered a {type} mineshaft!"

    @Expose
    @ConfigOption(
        name = "Mineshaft to Track",
        desc = "The mineshafts to send stats about for §e{sinceConfigShaft}§7 and §e{timeSinceConfigShaft}§7.\n" +
            "Other mineshafts will still have their data tracked, it just won't send stats for them."
    )
    @ConfigEditorDraggableList
    val mineshaftsToTrack: MutableList<MineshaftDetection.MineshaftTypes> = mutableListOf(
        MineshaftDetection.MineshaftTypes.FAIR1,
        MineshaftDetection.MineshaftTypes.JASP1
    )
}
