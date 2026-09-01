package at.hannibal2.skyhanni.config.features.slayer.vampire

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.generic.lineconfigs.SlayerLineConfigs
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class VampireConfig {
    @Expose
    @ConfigOption(name = "Your Boss", desc = "")
    @Accordion
    val ownBoss: OwnBossConfig = OwnBossConfig()

    @Expose
    @ConfigOption(name = "Others Boss", desc = "")
    @Accordion
    val othersBoss: OthersBossConfig = OthersBossConfig()

    @Expose
    @ConfigOption(name = "Co-op Boss", desc = "")
    @Accordion
    val coopBoss: CoopBossHighlightConfig = CoopBossHighlightConfig()

    @Expose
    @ConfigOption(name = "Line from Crosshair To Boss Head.", desc = "")
    @Accordion
    val line: SlayerLineConfigs.SlayerLineDefaultOff = SlayerLineConfigs.SlayerLineDefaultOff()

    @Expose
    @ConfigOption(name = "Transparency", desc = "Choose the transparency of the color.")
    @ConfigEditorSlider(minStep = 1f, minValue = 1f, maxValue = 250f)
    var withAlpha: Int = 80

    @Expose
    @ConfigOption(name = "Low Health", desc = "Change color when the boss is below 20% health.")
    @ConfigEditorBoolean
    @FeatureToggle
    var changeColorWhenCanSteak: Boolean = true

    @Expose
    @ConfigOption(name = "Can use Steak Color", desc = "Color when the boss is below 20% health.")
    @ConfigEditorColour
    var steakColor: ChromaColour = ChromaColour.fromStaticRGB(255, 0, 88, 255)

    @Expose
    @ConfigOption(name = "Twinclaws", desc = "Delay the Twinclaws alert for a given amount in milliseconds.")
    @ConfigEditorSlider(minStep = 1f, minValue = 0f, maxValue = 1000f)
    var twinclawsDelay: Int = 0

    @Expose
    @ConfigOption(name = "Blood Ichor", desc = "")
    @Accordion
    val bloodIchor: BloodIchorConfig = BloodIchorConfig()

    @Expose
    @ConfigOption(name = "Killer Spring", desc = "")
    @Accordion
    val killerSpring: KillerSpringConfig = KillerSpringConfig()

    @SkyHanniModule
    companion object {
        @HandleEvent
        private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            val path = "slayer.vampire"
            event.move(146, "$path.drawLine", "$path.line.showLine")
            event.move(146, "$path.lineColor", "$path.line.color")
            event.move(146, "$path.lineWidth", "$path.line.lineWidth")
        }
    }
}
