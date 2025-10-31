package at.hannibal2.hanni.config

import at.hannibal2.hanni.data.SackItem
import at.hannibal2.hanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import java.util.UUID

class SackData {
    @Expose
    var players: MutableMap<UUID, PlayerSpecific> = mutableMapOf()

    class PlayerSpecific {
        @Expose
        var profiles: MutableMap<String, ProfileSpecific> = mutableMapOf()
    }

    class ProfileSpecific {
        @Expose
        var sackContents: Map<NeuInternalName, SackItem> = mutableMapOf()
    }
}
