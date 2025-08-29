package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose

@SkyHanniModule
object DummyTracker {

    private val config get() = GardenApi.config.dicerRngDropTracker
    private val tracker = SkyHanniTracker("Dummy Tracker", { Data() }, { it.garden.dummyTracker }) {
        drawDisplay(it)
    }
    private var doCount: Boolean = false

    class Data : TrackerData() {

        override fun resetData() {
            count = 0
        }

        @Expose
        var count: Int = 0
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (isEnabled() && doCount) addCount()
    }

    private fun drawDisplay(data: Data) = buildList {
        add(Renderable.text("§6§lDummy Tracker").toSearchable())
        add(Renderable.text("Count: ${data.count}").toSearchable())
    }

    private fun addCount(addedCount: Int = 1) {
        tracker.modify { storage ->
            storage.count += addedCount
        }
    }

    init {
        tracker.initRenderer({ config.position }) { shouldShowDisplay() }
    }

    private fun shouldShowDisplay(): Boolean {
        return isEnabled()
    }

    private fun isEnabled() = GardenApi.config.dicerRngDropTracker.enabled

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetdummytracker") {
            description = "Resets the Dicer Drop Tracker"
            category = CommandCategory.USERS_RESET
            callback { tracker.resetCommand() }
        }
        event.register("shstartdummytracker") {
            description = "Resets the Dicer Drop Tracker"
            category = CommandCategory.USERS_RESET
            callback { doCount = true }
        }
        event.register("shstopdummytracker") {
            description = "Resets the Dicer Drop Tracker"
            category = CommandCategory.USERS_RESET
            callback { doCount = false }
        }
    }
}
