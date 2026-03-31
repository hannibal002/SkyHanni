package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose

@SkyHanniModule
object WartyCropTracker {

    private val config get() = GardenApi.config.wartyCropTracker

    private val patternGroup = RepoPattern.group("garden.warty")

    /**
     * REGEX-TEST: §6§lRARE CROP! §r§f§r§5Warty §r§b(Wart Eater Bonus)
     */
    private val wartyPattern by patternGroup.pattern(
        "drop",
        "§6§lRARE CROP! §r§f§r§5Warty §r§b\\(Wart Eater Bonus\\)",
    )

    val tracker = SkyHanniTracker(
        "Warty Crop Tracker",
        ::Data,
        { it.garden.wartyCropTracker },
        trackerConfig = { config.perTrackerConfig },
        customUptimeControl = true,
    ) {
        drawDisplay(it)
    }

    data class Data(
        @Expose
        var wartyCount: Int = 0,
    ) : TrackerData<SessionUptime.Garden>(SessionUptime.Garden::class)

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!wartyPattern.matches(event.message)) return
        tracker.modify { it.wartyCount++ }
        if (config.hideChat) {
            event.blockedReason = "warty_crop_drop"
        }
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§7Warty Crop Tracker:")
        addSearchString(" §7- §e${data.wartyCount.addSeparators()}x §5Warty")
    }

    init {
        tracker.initRenderer({ config.position }) { shouldShowDisplay() }
    }

    private fun shouldShowDisplay(): Boolean {
        if (!config.enabled) return false
        if (!GardenApi.inGarden()) return false
        val crop = GardenApi.cropInHand ?: return false
        if (crop != CropType.NETHER_WART) return false
        return true
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.GARDEN) {
            tracker.firstUpdate()
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetwartycroptracker") {
            description = "Resets the Warty Crop Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }
}
