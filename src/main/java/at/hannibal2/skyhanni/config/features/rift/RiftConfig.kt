package at.hannibal2.skyhanni.config.features.rift

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.features.rift.area.RiftAreasConfig
import at.hannibal2.skyhanni.config.features.rift.motes.MotesConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class RiftConfig {
    @ConfigOption(name = "Rift Timer", desc = "")
    @Accordion
    @Expose
    val timer: RiftTimerConfig = RiftTimerConfig()

    @ConfigOption(name = "Crux Talisman Progress", desc = "")
    @Accordion
    @Expose
    val cruxTalisman: CruxTalismanDisplayConfig = CruxTalismanDisplayConfig()

    @ConfigOption(name = "Enigma Soul Waypoints", desc = "")
    @Accordion
    @Expose
    val enigmaSoulWaypoints: EnigmaSoulConfig = EnigmaSoulConfig()

    @Category(name = "Rift Areas", desc = "Rift Area Settings")
    @Expose
    val area: RiftAreasConfig = RiftAreasConfig()

    @Expose
    @Category(name = "Motes", desc = "")
    val motes: MotesConfig = MotesConfig()

    @Expose
    @ConfigOption(name = "Motes Orbs", desc = "")
    @Accordion
    val motesOrbs: MotesOrbsConfig = MotesOrbsConfig() // TODO move into MotesConfig

    @Expose
    @ConfigOption(name = "Punchcard Artifact", desc = "")
    @Accordion
    val punchcard: PunchcardConfig = PunchcardConfig()

    @Expose
    @ConfigOption(name = "Highlight Guide", desc = "Highlight things to do in the Rift Guide.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightGuide: Boolean = true

    @Expose
    @ConfigOption(name = "Horsezooka Hider", desc = "Hide horses while holding the Horsezooka in the hand.")
    @ConfigEditorBoolean
    @FeatureToggle
    var horsezookaHider: Boolean = false

    @Expose
    @ConfigOption(
        name = "Temporal Pillar Dodge",
        desc = "Avoid pathfinding solutions through or near the big enderman " +
            "that throws you back to the end credits screen and steals so much time.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var temporalPillarDodge: Boolean = true
}
