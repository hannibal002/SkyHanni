package at.hannibal2.skyhanni.config.features.chat
import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SlayerFilterConfig {
    @Expose
    @ConfigOption(name = "Low Value Slayer Drops", desc = "Hide low value slayer drop message")
    @ConfigEditorBoolean
    @FeatureToggle
    var slayerDropMessages: Boolean = false

    @Expose
    @ConfigOption(name = "Slayer Quest Start/End messages", desc = "Hides useless Slayer Quest messages")
    @ConfigEditorBoolean
    @FeatureToggle
    var questMessages: Boolean = false
}
