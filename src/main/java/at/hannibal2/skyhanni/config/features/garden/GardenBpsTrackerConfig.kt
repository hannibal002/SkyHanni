package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.TopLevelTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.GardenTrackerSettings
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.TimedPerTrackerConfig
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class GardenBpsTrackerConfig : TopLevelTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Track crop block breaks in garden.")
    @ConfigEditorBoolean
    @FeatureToggle
    override var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Stats List", desc = "Drag text to change what displays in the summary card.")
    @ConfigEditorDraggableList
    val uptimeDisplayText: Property<MutableList<GardenUptimeDisplayText>> = Property.of(GardenUptimeDisplayText.defaultValues)

    @Expose
    @ConfigOption(name = "Tracker Settings", desc = "")
    @Accordion
    override val perTrackerConfig: TimedPerTrackerConfig<GardenTrackerSettings> = TimedPerTrackerConfig()

    enum class GardenUptimeDisplayText(private val displayName: String) {
        TITLE("Crop Break Tracker"),
        BPS("Blocks/Second: 17.11"),
        BLOCKS_BROKEN("Blocks Broken: 17,912"),
        ;

        override fun toString(): String = displayName

        companion object {
            @Suppress("StorageNeedsExpose")
            internal val defaultValues: MutableList<GardenUptimeDisplayText> = mutableListOf(
                TITLE,
                BPS,
                BLOCKS_BROKEN
            )
        }
    }

    @Expose
    @ConfigLink(owner = GardenBpsTrackerConfig::class, field = "enabled")
    override val position: Position = Position(5, -180)

    @SkyHanniModule
    companion object {
        @HandleEvent
        fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            val base = "garden.gardenBpsTracker"
            event.move(130, "$base.showDisplay", "$base.enabled")
            event.move(130, "$base.pos", "$base.position")
        }
    }
}
