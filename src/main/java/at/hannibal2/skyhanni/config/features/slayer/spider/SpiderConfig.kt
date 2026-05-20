package at.hannibal2.skyhanni.config.features.slayer.spider

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.generic.LineToConfig
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class SpiderConfig {

    @Expose
    @ConfigOption(name = "Line To Tarantula Boss", desc = "")
    @Accordion
    val lineToBoss: LineToConfig = LineToConfig(defaultColor = LorenzColor.AQUA.toChromaColor())

    @Expose
    @ConfigOption(name = "Mark When Invincible", desc = "Highlight the Tarantula Slayer tier 5 when the hatchlings are alive.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightInvincible: Boolean = true

    @Expose
    @ConfigOption(name = "Invincible Color", desc = "The color used to highlight the invincible phase.")
    @ConfigEditorColour
    val highlightInvincibleColor: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(255, 255, 0, 60))

    @Expose
    @ConfigOption(name = "Phase Display", desc = "Show the current phase of the Tara 5 Slayer boss.")
    @ConfigEditorBoolean
    var phaseDisplay: Boolean = false

    @SkyHanniModule
    companion object {
        @HandleEvent
        fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            event.move(134, "slayer.spider.lineToBoss", "slayer.spider.lineToBoss.showLine")
            event.move(134, "slayer.spider.slayerLineWidth", "slayer.spider.lineToBoss.slayerLineWidth")
        }
    }
}
