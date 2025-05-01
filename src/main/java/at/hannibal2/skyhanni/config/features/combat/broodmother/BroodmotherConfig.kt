package at.hannibal2.skyhanni.config.features.combat.broodmother

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.combat.BroodmotherFeatures.StageEntry
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import java.util.*

class BroodmotherConfig {
    @Expose
    @ConfigOption(
        name = "Countdown",
        desc = "Display a countdown until the Broodmother will spawn.\n" +
            "§cCountdown will not show unless the time until spawn has been established, and may be off by a few seconds."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var countdown: Boolean = true

    @Expose
    @ConfigOption(name = "Spawn Alert", desc = "Send a chat message, title and sound when the Broodmother spawns.")
    @ConfigEditorBoolean
    @FeatureToggle
    var alertOnSpawn: Boolean = false

    @Expose
    @ConfigOption(name = "Alert Settings", desc = "")
    @Accordion
    var spawnAlert: BroodmotherSpawnAlertConfig = BroodmotherSpawnAlertConfig()

    @Expose
    @ConfigOption(name = "Imminent Warning", desc = "Warns you when the Broodmother is 1 minute away from spawning.")
    @ConfigEditorBoolean
    @FeatureToggle
    var imminentWarning: Boolean = false

    @Expose
    @ConfigOption(
        name = "Chat Messages",
        desc = "Send a chat message when the Broodmother enters these stages.\n" +
            "§cThe 'Alive!' and 'Imminent' stages are overridden by the \"Spawn Alert\" and \"Imminent Warning\" features."
    )
    @ConfigEditorDraggableList
    var stages: MutableList<StageEntry> = mutableListOf(
        StageEntry.SLAIN,
        StageEntry.ALIVE
    )

    @Expose
    @ConfigOption(
        name = "Stage on Server Join",
        desc = "Send a chat message with the Broodmother's current stage upon joining the Spider's Den."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var stageOnJoin: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hide own kills",
        desc = "Disable the chat message for the §eSlain §rstage if at the Spider Mound."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideSlainWhenNearby: Boolean = false

    @Expose
    @ConfigLink(owner = BroodmotherConfig::class, field = "countdown")
    var countdownPosition: Position = Position(10, 10)
}
