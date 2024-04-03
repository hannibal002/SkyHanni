package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object PestProfitTracker {
    val config get() = SkyHanniMod.feature.garden.pests.pestProfitTacker

    private val patternGroup = RepoPattern.group("garden.pests.tracker")

    /**
     * REGEX-TEST: §eYou received §a7x Enchanted Potato §efor killing a §6Locust§e!
     * REGEX-TEST: §eYou received §a6x Enchanted Cocoa Beans §efor killing a §6Moth§e!
     */
    private val pestLootPattern by patternGroup.pattern(
        "loot",
        "§eYou received §a(?<amount>[0-9]*)x (?<item>.*) §efor killing an? §6(?<pest>.*)§e!"
    )

    /**
     * REGEX-TEST: §6§lRARE DROP! §9Mutant Nether Wart §6(§6+1,344☘)
     * REGEX-TEST: §6§lPET DROP! §r§5Slug §6(§6+1300☘)
     */
    private val pestRareDropPattern by patternGroup.pattern(
        "raredrop",
        "§6§l(?:RARE|PET) DROP! (?:§.)*(?<item>.+) §6\\(§6\\+.*☘\\)"
    )

    private var lastPestKillTime = SimpleTimeMark.farPast()
    private val tracker = SkyHanniItemTracker(
        "Pest Profit Tracker",
        { Data() },
        { it.garden.pestProfitTracker }) { drawDisplay(it) }

    class Data : ItemTrackerData() {
        override fun resetItems() {
            totalPestsKills = 0L
        }

        override fun getDescription(timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / totalPestsKills
            val dropRate = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate."
            )
        }

        override fun getCoinName(item: TrackedItem) = "§6Pest Kill Coins"

        override fun getCoinDescription(item: TrackedItem): List<String> {
            val pestsCoinsFormat = NumberUtil.format(item.totalAmount)
            return listOf(
                "§7Killing pests gives you coins.",
                "§7You got §6$pestsCoinsFormat coins §7that way."
            )
        }

        @Expose
        var totalPestsKills = 0L
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        pestLootPattern.matchMatcher(event.message) {
            val amount = group("amount").toInt()
            val internalName = NEUInternalName.fromItemNameOrNull(group("item")) ?: return

            tracker.addItem(internalName, amount)
            addKill()
            if (config.hideChat) event.blockedReason = "pest_drop"
        }
        pestRareDropPattern.matchMatcher(event.message) {
            val internalName = NEUInternalName.fromItemNameOrNull(group("item")) ?: return

            tracker.addItem(internalName, 1)
            // pests always have guaranteed loot, therefore there's no need to add kill here
        }
    }

    private fun addKill() {
        tracker.modify {
            it.totalPestsKills++
        }
        lastPestKillTime = SimpleTimeMark.now()
    }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        addAsSingletonList("§e§lPest Profit Tracker")
        val profit = tracker.drawItems(data, { true }, this)

        val pestsKilled = data.totalPestsKills
        addAsSingletonList(
            Renderable.hoverTips(
                "§7Pests killed: §e${pestsKilled.addSeparators()}",
                listOf("§7You killed pests §e${pestsKilled.addSeparators()} §7times.")
            )
        )
        addAsSingletonList(tracker.addTotalProfit(profit, data.totalPestsKills, "kill"))

        tracker.addPriceFromButton(this)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (GardenAPI.isCurrentlyFarming()) return
        if (lastPestKillTime.passedSince() > config.timeDisplayed.seconds && !PestAPI.hasVacuumInHand()) return

        tracker.renderDisplay(config.position)
    }

    @SubscribeEvent
    fun onPurseChange(event: PurseChangeEvent) {
        if (!isEnabled()) return
        val coins = event.coins
        if (event.reason == PurseChangeCause.GAIN_MOB_KILL && lastPestKillTime.passedSince() < 2.seconds) {
            tracker.addCoins(coins.toInt())
        }
    }

    fun resetCommand(args: Array<String>) {
        tracker.resetCommand(args, "shresetpestprofittracker")
    }

    fun isEnabled() = GardenAPI.inGarden() && config.enabled
}
