package at.hannibal2.skyhanni.config.features.dungeon

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.generic.lineconfigs.LineToFelSkull
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class ObjectHighlighterConfig {
    // TODO move some stuff from DungeonConfig into this
    @Expose
    @ConfigOption(name = "Starred Mobs", desc = "")
    @Accordion
    val starred: StarredConfig = StarredConfig()

    class StarredConfig {
        @Expose
        @ConfigOption(name = "Highlight Starred", desc = "Highlights starred mobs in a color.")
        @ConfigEditorBoolean
        @FeatureToggle
        val highlight: Property<Boolean> = Property.of(false)

        /*
        TODO for someone who has time
        @Expose
        @ConfigOption(name = "Show Outline", desc = "Shows only an outline instead of a full highlight.")
        @ConfigEditorBoolean
        public Property<Boolean> showOutline = Property.of(true); */
        @ConfigOption(name = "No Chroma", desc = "§cThe chroma setting for the color is currently not working!")
        @ConfigEditorInfoText
        val info: String? = null

        @Expose
        @ConfigOption(name = "Color", desc = "The color used to highlight starred mobs.")
        @ConfigEditorColour
        val color: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(255, 255, 0, 60))
    }

    @Expose
    @ConfigOption(name = "Fels Skull", desc = "")
    @Accordion
    val fel: FelConfig = FelConfig()

    class FelConfig {
        @Expose
        @ConfigOption(name = "Line To Fel Skulls", desc = "")
        @Accordion
        val line: LineToFelSkull = LineToFelSkull()

        @Expose
        @ConfigOption(name = "Highlight Fels Skull", desc = "Highlights fels that are not active uses same Color as above line.")
        @ConfigEditorBoolean
        @FeatureToggle
        val highlight: Property<Boolean> = Property.of(true)
    }

    @SkyHanniModule
    companion object {
        @HandleEvent
        private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            val path = "dungeon.objectHighlighter"
            event.move(146, "$path.fel.line", "$path.fel.line.showLine")
            event.move(146, "$path.fel.color", "$path.fel.line.color")
        }
    }
}
