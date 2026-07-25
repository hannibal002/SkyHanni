package at.hannibal2.skyhanni.config.features.dungeon

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class MessageFilterConfig {
    @Expose
    @ConfigOption(name = "Rare Drops", desc = "Hide the chat message when other players get rare drops from chests.")
    @ConfigEditorBoolean
    @FeatureToggle
    val rareDrops: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Keys and Doors",
        desc = "Hide the chat message when picking up keys or opening doors in Dungeons."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val keysAndDoors: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Solo Class", desc = "Hide the message that sends when you play a class alone.")
    @ConfigEditorBoolean
    @FeatureToggle
    val soloClass: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Solo Class Stats", desc = "Hide the boosted class stats when starting a dungeon.")
    @ConfigEditorBoolean
    @FeatureToggle
    val soloStats: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Fairy Dialogue", desc = "Hide the dialogue when a fairy is killed.")
    @ConfigEditorBoolean
    @FeatureToggle
    val fairy: Property<Boolean> = Property.of(false)
}
