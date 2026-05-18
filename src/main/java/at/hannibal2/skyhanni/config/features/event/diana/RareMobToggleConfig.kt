package at.hannibal2.skyhanni.config.features.event.diana

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class RareMobToggleConfig {

    @Expose
    @ConfigOption(name = "Sphinx", desc = "Toggle waypoints for §6Sphinx§7.")
    @ConfigEditorBoolean
    var sphinx: Boolean = true

    @Expose
    @ConfigOption(name = "Minos Inquisitor", desc = "Toggle waypoints for §6Minos Inquisitor§7.")
    @ConfigEditorBoolean
    var minosInquisitor: Boolean = true

    @Expose
    @ConfigOption(name = "Manticore", desc = "Toggle waypoints for §6Manticore§7.")
    @ConfigEditorBoolean
    var manticore: Boolean = true

    @Expose
    @ConfigOption(name = "King Minos", desc = "Toggle waypoints for §6King Minos§7.")
    @ConfigEditorBoolean
    var kingMinos: Boolean = true
}
