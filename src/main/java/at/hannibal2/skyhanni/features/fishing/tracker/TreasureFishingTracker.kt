package at.hannibal2.skyhanni.features.fishing.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.fishing.FishingBobberCastEvent
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

@SkyHanniModule
object TreasureFishingTracker {

    private val config get() = SkyHanniMod.feature.fishing.treasureFishingTracker

    /**
     * REGEX-TEST: §5⛃ §r§5§lGOOD CATCH! §r§fYou caught §r§bIce Essence §r§8x5§r§f!
     * REGEX-TEST: §5⛃ §r§5§lGOOD §r§2§lJUNK§r§5§l CATCH! §r§fYou caught a §r§aRusty Coin§r§f!§r§7
     * REGEX-TEST: §6⛃ §r§6§lGREAT CATCH! §r§fYou caught §r§aEnchanted Snow Block §r§8x8§r§f!
     * REGEX-TEST: §6⛃ §r§6§lGREAT §r§2§lJUNK§r§6§l CATCH! §r§fYou caught a §r§9Busted Belt Buckle§r§f!
     * REGEX-TEST: §d⛃ §r§d§lOUTSTANDING CATCH! §r§fYou caught §r§9Enchanted Fig Log §r§8x10§r§f!
     * REGEX-TEST: §d⛃ §r§d§lOUTSTANDING §r§2§lJUNK§r§d§l CATCH! §r§fYou caught an §r§fOld Boot§r§f!
     * REGEX-FAIL: ⛃ GOOD CATCH! You caught Ice Essence x5!
     * REGEX-FAIL: §5⛃ §r§5§lFAIR CATCH! §r§fYou caught §r§bIce Essence §r§8x5§r§f!
     * REGEX-FAIL: §5⛃ §r§5§lGOOD CATCH! §r§fYou found §r§bIce Essence §r§8x5§r§f!
     */
    private val treasureCatchPattern by RepoPattern.pattern(
        "fishing.tracker.treasure.catch",
        "^§[56d]⛃ §r§[56d]§l(?<catchType>GOOD|GREAT|OUTSTANDING)" +
            "(?: §r§2§lJUNK§r§[56d]§l)? CATCH! §r§fYou caught (?:an? )?§r.*$",
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
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return

        val message = event.messageComponent.getText().removePrefix("§r").trim()
        treasureCatchPattern.matchMatcher(message) {
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
    fun onBobberThrow(event: FishingBobberCastEvent) {
        tracker.firstUpdate()
        if (config.enabled && isEnabled()) tracker.startSessionUptime()
    }

    @HandleEvent
    fun onConfigLoad() {
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
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresettreasurefishingtracker") {
            description = "Resets the Treasure Fishing Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }
}
