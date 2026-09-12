package at.hannibal2.skyhanni.config.features.mining.glacite

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
    val types: MineshaftWaypointTypesConfig = MineshaftWaypointTypesConfig()

    @Expose
    @ConfigOption(
        name = "All Corpses Found Alert",
        desc = "Alert when all corpses in the Mineshaft have been found.",
    )
    @ConfigEditorBoolean
    var allCorpsesFoundAlert: Boolean = true

    @Expose
    @ConfigOption(
        name = "Auto Share Corpses",
        desc = "Automatically share the location and type of found corpses in party chat.",
    )
    @ConfigEditorBoolean
    var autoShareCorpses: Boolean = false

    @Expose
    @ConfigOption(
        name = "Share Corpse Keybind",
        desc = "Share the location of the nearest found corpse upon key press.\n" +
            "§eYou can share the location even if it has already been shared!",
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var shareCorpseKeybind: Int = GLFW.GLFW_KEY_UNKNOWN

    @SkyHanniModule
    companion object {
        @HandleEvent
        private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
            val basePath = "mining.glaciteMineshaft"
            event.move(140, "$basePath.mineshaftWaypoints.entranceLocation", "$basePath.waypointsConfig.types.entrance")
            event.move(140, "$basePath.mineshaftWaypoints.ladderLocation", "$basePath.waypointsConfig.types.ladder")
            event.move(140, "$basePath.corpseLocator.enabled", "$basePath.waypointsConfig.types.foundCorpse") {
                event.add(140, "$basePath.waypointsConfig.types.lootedCorpse") { it }
                it
            }
            event.move(140, "$basePath.corpseLocator.autoSendLocation", "$basePath.waypointsConfig.autoShareFoundCorpses")
            event.move(140, "$basePath.shareWaypointLocation", "$basePath.waypointsConfig.shareFoundCorpseKeybind")
            event.remove(140, "$basePath.mineshaftWaypoints.enabled")
        }
    }
}
