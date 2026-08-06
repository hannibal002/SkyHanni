package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.config.core.config.Position
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
    var graphEditorTutorialAcknowledged: Boolean = false

    @Expose
    var harvestFeastStorage: HarvestFeastStorage = HarvestFeastStorage()

    @Expose
    val trackerDisplayModes: MutableMap<String, SkyHanniTracker.DisplayMode> = mutableMapOf()

    @Expose
    var foundDianaBurrowLocations: List<LorenzVec> = emptyList()

    @Expose
    val players: MutableMap<UUID, PlayerSpecificStorage> = mutableMapOf()

    @Expose
    val blacklistedUsers: MutableList<String> = mutableListOf()

    @Expose
    val reminders: MutableMap<String, Reminder> = mutableMapOf()

    @Expose
    val testRenderablePositions: MutableMap<String, Position> = mutableMapOf()
}
