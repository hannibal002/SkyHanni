package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyModern
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class StarlynSistersConfig {

    @Expose
    @ConfigOption(name = "Compact Results", desc = "Compacts the announcements for your placement in a §dStarlyn Sister §7contest.")
    @ConfigEditorBoolean
    @FeatureToggle
    @OnlyModern
    var compactResults = false

    @Expose
    @Suppress("MaxLineLength") // I genuinely do not know how to make the description any shorter. formatting codes won't change anything
    @ConfigOption(name = "Compact Personal Bests", desc = "Compact messages from reaching new log collection §dpersonal bests §7during contests.")
    @ConfigEditorBoolean
    @FeatureToggle
    @OnlyModern
    var compactPersonalBest = false

}