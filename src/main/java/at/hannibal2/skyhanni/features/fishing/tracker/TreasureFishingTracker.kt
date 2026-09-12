package at.hannibal2.skyhanni.features.fishing.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import com.mojang.brigadier.arguments.LongArgumentType

@SkyHanniModule
object TreasureFishingTracker {

    private val config get() = SkyHanniMod.feature.fishing.treasureFishingTracker

    /**
     * REGEX-TEST:  GOOD CATCH! You caught Ice Essence x5!
     * REGEX-TEST:  GOOD JUNK CATCH! You caught a Rusty Coin!
     * REGEX-TEST:  GREAT CATCH! You caught Enchanted Snow Block x8!
     * REGEX-TEST:  GREAT JUNK CATCH! You caught a Busted Belt Buckle!
     * REGEX-TEST:  OUTSTANDING CATCH! You caught Enchanted Fig Log x10!
     * REGEX-TEST:  OUTSTANDING JUNK CATCH! You caught an Old Leather Boot!
     */
    private val treasureCatchPattern by RepoPattern.pattern(
        "fishing.tracker.treasure.catch",
        "^${SkyblockStat.TREASURE_CHANCE.hypixelIcon} (?<catchType>GOOD|GREAT|OUTSTANDING)(?: JUNK)? CATCH! You caught (?:an? )?.*$",
    )

    private val tracker = SkyHanniTracker(
        "Treasure Fishing Tracker",
        ::Data,
        { it.fishing.treasureFishingTracker },
        trackerConfig = { config.perTrackerConfig },
    ) { drawDisplay(it) }

    init {
        tracker.initRenderer({ config.position }) { shouldShowDisplay() }
    }

    data class Data(
        @Expose var catchAmounts: MutableMap<TreasureCatch, Long> = enumMapOf(),
    ) : TrackerData<SessionUptime.Normal>(SessionUptime.Normal::class)

    enum class TreasureCatch(
        val displayName: String,
        val color: String,
    ) {
        GOOD("Good", "§5"),
        GREAT("Great", "§6"),
        OUTSTANDING("Outstanding", "§d"),
    }

    @HandleEvent
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return

        treasureCatchPattern.matchMatcher(event.cleanMessage.trim()) {
            val catch = TreasureCatch.valueOf(group("catchType"))
            tracker.modify { it.catchAmounts.addOrPut(catch, 1) }
        }
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§7Treasure Fishing Tracker:")

        val total = data.catchAmounts.values.sum()

        for (catch in TreasureCatch.entries) {
            val amount = data.catchAmounts[catch] ?: 0
            val percentageSuffix = if (config.showPercentage.get()) {
                val percentage = if (total > 0) (amount.toDouble() / total).formatPercentage() else 0.0.formatPercentage()
                " §7$percentage"
            } else ""

            addSearchString(
                " §7- §e${amount.addSeparators()} ${catch.color}${catch.displayName} §7Catches$percentageSuffix",
                catch.displayName,
            )
        }

        addSearchString(" §7- §e${total.addSeparators()} §7Total Catches")
    }

    @HandleEvent
    private fun onBobberCast() {
        tracker.firstUpdate()
        if (config.enabled && isEnabled()) tracker.startSessionUptime()
    }

    @HandleEvent
    private fun onConfigLoad() {
        ConditionalUtils.onToggle(config.showPercentage) {
            tracker.update()
        }
    }

    private fun shouldShowDisplay(): Boolean {
        if (!config.enabled) return false
        if (!isEnabled()) return false
        if (!FishingApi.isFishing(checkRodInHand = false)) return false

        return true
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock &&
        !IslandType.GARDEN.isInIsland() &&
        !KuudraApi.inKuudra

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresettreasurefishingtracker") {
            description = "Resets the Treasure Fishing Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
        event.registerBrigadier("shaddtreasurefishingcatch") {
            description = "Adds a catch to the Treasure Fishing Tracker"
            category = CommandCategory.USERS_BUG_FIX
            arg("catchType", EnumArgumentType.custom<TreasureCatch>({ it.displayName })) { typeRef ->
                callback {
                    tracker.modify { it.catchAmounts.addOrPut(getArg(typeRef), 1) }
                }
                argCallback("amount", LongArgumentType.longArg()) { amount ->
                    tracker.modify { it.catchAmounts.addOrPut(getArg(typeRef), amount) }
                }
            }
        }
    }
}
