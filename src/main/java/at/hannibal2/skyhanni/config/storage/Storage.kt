package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.features.misc.reminders.Reminder
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.annotations.Expose
import java.util.UUID

class Storage {
    @Expose
    var hasPlayedBefore: Boolean = false

    @Expose
    var visualWordsImported: Boolean = false

    @Expose
    var contestSendingAsked: Boolean = false

    @Expose
    var trackerDisplayModes: MutableMap<String, SkyHanniTracker.DisplayMode> = mutableMapOf()

    @Expose
    var foundDianaBurrowLocations: List<LorenzVec> = emptyList()

    @Expose
    var players: MutableMap<UUID, PlayerSpecificStorage> = mutableMapOf()

    @Expose
    var blacklistedUsers: MutableList<String> = mutableListOf()

    @Expose
    var reminders: MutableMap<String, Reminder> = mutableMapOf()
}
