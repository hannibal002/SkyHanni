package at.hannibal2.skyhanni.config.features.mining.glacite.waypoints

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class MineshaftWaypointsConfig {
    @Expose
    @ConfigOption(name = "Waypoint Types", desc = "")
    @Accordion
    var types: WaypointTypesConfig = WaypointTypesConfig()

    @Expose
    @ConfigOption(
        name = "Auto Share Corpses",
        desc = "Automatically share the location and type of found corpses in party chat.",
    )
    @ConfigEditorBoolean
    var autoShareFoundCorpses: Boolean = false

    @Expose
    @ConfigOption(
        name = "Share Corpse Keybind",
        desc = "Share the location of the nearest found corpse upon key press.\n" +
            "§eYou can share the location even if it has already been shared!",
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var shareFoundCorpseKeybind: Int = GLFW.GLFW_KEY_UNKNOWN

    @SkyHanniModule
    companion object {
        @HandleEvent
        fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            event.move(
                140,
                "mining.glaciteMineshaft.mineshaftWaypoints.entranceLocation",
                "mining.glaciteMineshaft.waypointsConfig.types.entrance",
            )
            event.move(
                140,
                "mining.glaciteMineshaft.mineshaftWaypoints.ladderLocation",
                "mining.glaciteMineshaft.waypointsConfig.types.ladder",
            )
            event.move(
                140,
                "mining.glaciteMineshaft.corpseLocator.enabled",
                "mining.glaciteMineshaft.waypointsConfig.types.foundCorpse",
            )
            event.move(
                140,
                "mining.glaciteMineshaft.corpseLocator.autoSendLocation",
                "mining.glaciteMineshaft.waypointsConfig.autoShareFoundCorpses",
            )
            event.move(
                140,
                "mining.glaciteMineshaft.shareWaypointLocation",
                "mining.glaciteMineshaft.waypointsConfig.shareFoundCorpseKeybind",
            )
        }
    }
}
