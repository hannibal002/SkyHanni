package at.hannibal2.hanni.config.storage

import at.hannibal2.hanni.config.core.config.Position
import at.hannibal2.hanni.features.misc.reminders.Reminder
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.tracker.HanniTracker
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
    var trackerDisplayModes: MutableMap<String, HanniTracker.DisplayMode> = mutableMapOf()

    @Expose
    var foundDianaBurrowLocations: List<LorenzVec> = emptyList()

    @Expose
    var players: MutableMap<UUID, PlayerSpecificStorage> = mutableMapOf()

    @Expose
    var blacklistedUsers: MutableList<String> = mutableListOf()

    @Expose
    var reminders: MutableMap<String, Reminder> = mutableMapOf()

    @Expose
    var testRenderablePositions: MutableMap<String, Position> = mutableMapOf()
}
