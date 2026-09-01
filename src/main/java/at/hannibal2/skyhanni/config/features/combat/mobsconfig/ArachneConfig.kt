package at.hannibal2.skyhanni.config.features.combat.mobsconfig

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator.replaceWithBoolean
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.generic.lineconfigs.LineToArachne
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ArachneConfig {

    @Expose
    @ConfigOption(name = "Boss Settings", desc = "")
    @Accordion
    val boss: ArachneBossConfig = ArachneBossConfig()

    class ArachneBossConfig {
        @Expose
        @ConfigOption(name = "Line To Arachne", desc = "")
        @Accordion
        val line: LineToArachne = LineToArachne()

        @Expose
        @ConfigOption(name = "Highlight", desc = "Highlight the Arachne boss in red.")
        @ConfigEditorBoolean
        @FeatureToggle
        var bossHighlight: Boolean = true

        @Expose
        @ConfigOption(
            name = "Arachne Spawn Timer",
            desc = "Show a timer when Arachne fragments or crystals are placed to indicate how long until the boss will spawn. " +
                "§eTimer may be 1 - 2 seconds off.",
        )
        @ConfigEditorBoolean
        @FeatureToggle
        var showSpawnTimer: Boolean = true

        @Expose
        @ConfigOption(
            name = "Arachne Kill Timer",
            desc = "Shows how long it took to kill Arachne after the fight ends. " +
                "§cDoes not show if you were not in the Sanctuary when it spawned.",
        )
        @ConfigEditorBoolean
        @FeatureToggle
        var killTimer: Boolean = true

        @Expose
        @ConfigOption(name = "Arachne Broods Name Hider", desc = "Hides the nametag above Arachne's Broods.")
        @ConfigEditorBoolean
        @FeatureToggle
        var hideNameTagOfBroods: Boolean = true

        @Expose
        @ConfigOption(name = "Arachne Brood Highlight", desc = "Highlights Arachne's Broods in Gold.")
        @ConfigEditorBoolean
        @FeatureToggle
        var broodHighlight: Boolean = true

    }

    @Expose
    @ConfigOption(name = "Keeper Settings", desc = "")
    @Accordion
    val keeper: ArachneKeeperConfig = ArachneKeeperConfig()

    class ArachneKeeperConfig {
        @Expose
        @ConfigOption(name = "Arachne Keeper", desc = "Highlight the Arachne Keeper in the Spider's Den in purple color.")
        @ConfigEditorBoolean
        @FeatureToggle
        var arachneKeeperHighlight: Boolean = true
    }

    @SkyHanniModule
    companion object {
        @HandleEvent
        private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            val oldArachnePath = "combat.mobs"
            val newArachneBossPath = "$oldArachnePath.arachneSettings.boss"
            val newArachneKeeperPath = "$oldArachnePath.arachneSettings.keeper"
            event.move(146, "$oldArachnePath.lineToArachne", "$newArachneBossPath.line.showLine")
            event.move(146, "$oldArachnePath.lineToArachneWidth", "$newArachneBossPath.line.lineWidth")
            event.move(146, "$oldArachnePath.showArachneSpawnTimer", "$newArachneBossPath.showSpawnTimer")
            event.move(146, "$oldArachnePath.arachneKillTimer", "$newArachneBossPath.killTimer")
            event.move(146, "$oldArachnePath.hideNameTagArachneMinis", "$newArachneBossPath.hideNameTagOfBroods")
            event.move(146, "$oldArachnePath.arachneKeeperHighlight", "$newArachneKeeperPath.arachneKeeperHighlight")
            event.transform(146, "${oldArachnePath}.arachneBossHighlighter") { element ->
                val enabled = JsonPrimitive(element.asString != "OFF")
                event.add(146, "$newArachneBossPath.bossHighlight") {
                    enabled
                }
                event.add(146, "$newArachneBossPath.broodHighlight") {
                    enabled
                }
                element
            }
        }
    }
}
