package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CombatFilterConfig {

    @Expose
    @ConfigOption(
        name = "Arachne Hider",
        desc = "Hide chat messages about the Arachne Fight while outside of §eArachne's Sanctuary§7.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideArachneMessages: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hide Far Deaths",
        desc = "Hide other players' death messages when they're not nearby (except during Dungeons/Kuudra fights)",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideFarDeathMessages: Boolean = false

    @Expose
    @ConfigOption(name = "Implosion Hider", desc = "Hide implosion ability messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var implosion: Boolean = false

    @Expose
    @ConfigOption(name = "Kill Combo", desc = "Hide messages about your Kill Combo from the Grandma Wolf pet.")
    @ConfigEditorBoolean
    @FeatureToggle
    var killCombo: Boolean = false

    @Expose
    @ConfigOption(name = "Midas Staff", desc = "Hide the Midas Staff molten wave ability messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var moltenWave: Boolean = false


    @Expose
    @ConfigOption(name = "Spirit Sceptre", desc = "Hide spirit sceptre ability messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var spiritSceptre: Boolean = false
}
