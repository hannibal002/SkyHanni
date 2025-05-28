package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.data.model.SkyHanniInventoryContainer
import com.google.gson.annotations.Expose
import java.util.NavigableMap
import java.util.TreeMap
import java.util.UUID

class StorageData {
    @Expose
    var players: MutableMap<UUID, PlayerSpecific> = mutableMapOf()

    class PlayerSpecific {
        @Expose
        var profiles: MutableMap<String, ProfileSpecific> = mutableMapOf()
    }

    class ProfileSpecific {
        @Expose
        var data: NavigableMap<String, SkyHanniInventoryContainer> = TreeMap()
    }
}
