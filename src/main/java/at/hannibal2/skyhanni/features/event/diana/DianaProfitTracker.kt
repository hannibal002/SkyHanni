package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ElectionApi.getElectionYear
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.jsonobjects.repo.DianaDropsJson
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.itemabilities.CrownOfAvariceCounter.isAvariceConsuming
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.annotations.Expose

@SkyHanniModule
object DianaProfitTracker {

    private val config get() = SkyHanniMod.feature.event.diana.dianaProfitTracker
    private var allowedDrops = listOf<NeuInternalName>()

    private val patternGroup = RepoPattern.group("diana.chat")

    /**
     * REGEX-TEST: You dug out a Griffin Burrow!
     * REGEX-TEST: You finished the Griffin burrow chain! (4/4)
     */
    private val chatDugOutPattern by patternGroup.pattern(
        "burrow.dug",
        "(?:You dug out a Griffin Burrow!|You finished the Griffin burrow chain!).*",
    )

    /**
     * REGEX-TEST: Wow! You dug out 1,000 coins!
     */
    private val chatDugOutCoinsPattern by patternGroup.pattern(
        "coins",
        "Wow! You dug out (?<coins>[\\d,.]+) coins!",
    )

    /**
     * REGEX-TEST: RARE DROP! You dug out a Griffin Feather!
     */
    private val griffinFeatherDropPattern by patternGroup.pattern(
        "griffin.feather.drop",
        "RARE DROP! You dug out a Griffin Feather!",
    )

    /**
     * REGEX-TEST: Follow the arrows to find the treasure!
     */
    private val treasureArrowPattern by patternGroup.pattern(
        "treasure.arrow",
        "Follow the arrows to find the treasure!",
    )

    private val tracker = SkyHanniItemTracker(
        "Diana Profit Tracker",
        ::Data,
        { it.diana.profitTracker },
        extraDisplayModes = mapOf(
            SkyHanniTracker.DisplayMode.MAYOR to {
                it.diana.profitTrackerPerElection.getOrPut(
                    SkyBlockTime.now().getElectionYear(), ::Data,
                )
            },
        ),
        drawDisplay = { drawDisplay(it) },
        trackerConfig = { config.perTrackerConfig }
    )

    data class Data(
        @Expose var burrowsDug: Long = 0,
    ) : ItemTrackerData<SessionUptime.Normal>(SessionUptime.Normal::class) {
        override fun getDescription(timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / burrowsDug
            val perBurrow = percentage.coerceAtMost(1.0).formatPercentage()

            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop chance per burrow: §c$perBurrow",
            )
        }

        override fun getCoinName(item: TrackedItem) = "§6Dug Out Coins"

        override fun getCoinDescription(item: TrackedItem): List<String> {
            val burrowDugCoinsFormat = item.totalAmount.shortFormat()
            return listOf(
                "§7Digging treasures gave you",
                "§6$burrowDugCoinsFormat coins §7in total.",
            )
        }
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§e§lDiana Profit Tracker")

        val profit = tracker.drawItems(data, { true }, this)

        val treasureCoins = data.burrowsDug
        add(
            Renderable.hoverTips(
                "§7Burrows dug: §e${treasureCoins.addSeparators()}",
                listOf("§7You dug out griffin burrows §e${treasureCoins.addSeparators()} §7times."),
            ).toSearchable(),
        )

        val duration = data.getTotalUptime()
        addAll(tracker.addTotalProfit(profit, data.burrowsDug, "burrow", duration, "Burrows", false))

        tracker.addPriceFromButton(this)
    }

    @HandleEvent
    private fun onItemAdd(event: ItemAddEvent) {
        if (!(DianaApi.isDoingDiana())) return
        val isCommand = event.source == ItemAddManager.Source.COMMAND
        if (isCommand && !config.enabled) return

        tryAddItem(event.internalName, event.amount, isCommand)
    }

    private fun tryAddItem(internalName: NeuInternalName, amount: Int, command: Boolean) {
        if (!isAllowedItem(internalName) && internalName != NeuInternalName.SKYBLOCK_COIN) {
            ChatUtils.debug("Ignored non-diana item pickup: '$internalName'")
            return
        }

        tracker.addItem(internalName, amount, command)
    }

    @HandleEvent(onlyOnIsland = HUB)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.cleanMessage
        if (chatDugOutPattern.matches(message)) {
            DianaApi.overrideActiveRitual()
            BurrowApi.lastBurrowRelatedChatMessage = SimpleTimeMark.now()
            tracker.modify {
                it.burrowsDug++
            }
            tryHide(event)
        }
        chatDugOutCoinsPattern.matchMatcher(message) {
            DianaApi.overrideActiveRitual()
            if (!isAvariceConsuming()) {
                BurrowApi.lastBurrowRelatedChatMessage = SimpleTimeMark.now()
                tryAddItem(NeuInternalName.SKYBLOCK_COIN, group("coins").formatInt(), command = false)
                tryHide(event)
            }
        }


        if (griffinFeatherDropPattern.matches(message) ||
            treasureArrowPattern.matches(message)
        ) {
            DianaApi.overrideActiveRitual()
            BurrowApi.lastBurrowRelatedChatMessage = SimpleTimeMark.now()
            tryHide(event)
        }
    }

    private fun tryHide(event: SkyHanniChatEvent.Allow) {
        if (SkyHanniMod.feature.chat.filterType.diana) {
            event.blockedReason = "diana_chain_or_drops"
        }
    }

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { config.enabled && (DianaApi.isDoingDiana() || DianaApi.hasSpadeInHand()) },
            onRender = {
                if (DianaApi.hasSpadeInHand()) tracker.firstUpdate()
                tracker.renderDisplay(config.position)
            },
        )
    }

    private fun isAllowedItem(internalName: NeuInternalName): Boolean = internalName in allowedDrops

    @HandleEvent
    private fun onRepoReload(event: RepositoryReloadEvent) {
        allowedDrops = event.getConstant<DianaDropsJson>("DianaDrops").dianaDrops
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetdianaprofittracker") {
            description = "Resets the Diana Profit Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }

    private val migrationMapping by lazy {
        mapOf(
            "dianaProfitTracker" to "profitTracker",
            "dianaProfitTrackerPerElectionSeason" to "profitTrackerPerElection",
            "mythologicalMobTrackerPerElectionSeason" to "mythologicalMobTrackerPerElection",
        )
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        migrationMapping.forEach { (old, new) ->
            event.move(70, "#profile.diana.$old", "#profile.diana.$new")
        }
    }
}
