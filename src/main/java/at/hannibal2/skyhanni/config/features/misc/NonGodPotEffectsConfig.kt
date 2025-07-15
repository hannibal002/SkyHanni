package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class NonGodPotEffectsConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Display the active potion effects that are not part of the God Pot."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    @SearchTag("Pest Repellent")
    var displayEnabled: Boolean = false

    @Expose
    @ConfigOption(name = "Show Mixins", desc = "Include God Pot mixins in the Non God Pot Effects display.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showMixins: Boolean = false

    @Expose
    @ConfigOption(name = "Expire Warning", desc = "Sends a title when one of the Non God Pot Effects is expiring.")
    @ConfigEditorBoolean
    @FeatureToggle
    var expireWarning: Boolean = false

    @Expose
    @ConfigOption(name = "Expire Sound", desc = "Makes a sound when one of the Non God Pot Effects is expiring.")
    @ConfigEditorBoolean
    @FeatureToggle
    var expireSound: Boolean = false

    @Expose
    @ConfigOption(
        name = "Expire Warning Time",
        desc = "Change the time in seconds before the potion expries to warn you."
    )
    @ConfigEditorSlider(minValue = 30f, maxValue = 300f, minStep = 5f)
    var expireWarnTime: Int = 30

    @Expose
    @ConfigLink(owner = NonGodPotEffectsConfig::class, field = "displayEnabled")
    val position: Position = Position(10, 10)
}
