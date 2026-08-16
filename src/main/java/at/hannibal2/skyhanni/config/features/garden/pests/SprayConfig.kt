package at.hannibal2.skyhanni.config.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SprayConfig {

    @Expose
    @ConfigOption(name = "Spray Display", desc = "")
    @Accordion
    val SprayDisplay: SprayDisplayConfig = SprayDisplayConfig()

    @Expose
    @ConfigOption(name = "Draw Plot Border", desc = "Draw plots border when holding the Sprayonator.")
    @ConfigEditorBoolean
    @FeatureToggle
    var drawPlotsBorderWhenInHands: Boolean = true

    @Expose
    @ConfigOption(
        name = "Spray Expiration Notice",
        desc = "Show a notification in chat when a spray runs out in any plot. Only active in Garden.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var expiryNotification: Boolean = true

    @Expose
    @ConfigOption(name = "New Spray Notice", desc = "Send a message in chat if a new spray is detected when entering a plot.")
    @ConfigEditorBoolean
    @FeatureToggle
    var newSprayNotification: Boolean = false

    @SkyHanniModule
    companion object {

        @HandleEvent
        private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            val path = "garden.pests.spray."
            event.move(144, "${path}displayEnabled", "${path}SprayDisplay.displayEnabled")
            event.move(144, "${path}showNotSprayed", "${path}SprayDisplay.showNotSprayed")
            event.move(144, "${path}displayPosition", "${path}SprayDisplay.displayPosition")
        }
    }
}
