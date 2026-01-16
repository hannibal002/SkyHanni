package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui.SpecificSeaCreatureSettings
import com.google.gson.annotations.Expose

class SpecificSeaCreatureStorage {
    @Expose
    var specificSeaCreatureConfigStorage: MutableMap<String, SpecificSeaCreatureSettings> = mutableMapOf()
}
