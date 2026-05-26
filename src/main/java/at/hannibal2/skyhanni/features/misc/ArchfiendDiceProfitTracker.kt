package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ArchfiendDiceProfitTracker {

    private val config get() = SkyHanniMod.feature.misc.archfiendDiceProfitTracker

    /**
     * REGEX-TEST: §eYour §r§5Archfiend Dice §r§erolled a §r§56§r§e! Bonus: §r§c12❤
     * REGEX-TEST: §eYour §r§6High Class Archfiend Dice §r§erolled a §r§67§r§e! Bonus: §r§a24❤
     */
    private val diceRollChatPattern by RepoPattern.pattern(
        "data.itemmanager.diceroll",
        "§eYour §r§[56](?<isHighClass>High Class )?Archfiend Dice §r§erolled a §r§.(?<number>[1-7])§r§e! Bonus: §r§.(?<hearts>.*)❤",
    )

    private val ARCHFIEND_DICE = "ARCHFIEND_DICE".toInternalName()
    private val HIGH_CLASS_ARCHFIEND_DICE = "HIGH_CLASS_ARCHFIEND_DICE".toInternalName()
    private val ARCHFIEND_DYE = "DYE_ARCHFIEND".toInternalName()

    // Constants for roll costs and win amounts (in coins)
    private const val ARCHFIEND_DICE_ROLL_COST = 666_000L
    private const val ARCHFIEND_DICE_WIN_COST = 15_000_000L
    private const val HIGH_CLASS_ARCHFIEND_DICE_ROLL_COST = 6_600_000L
    private const val HIGH_CLASS_ARCHFIEND_DICE_WIN_COST = 100_000_000L

    private var lastDiceRoll = SimpleTimeMark.farPast()
    private val tracker = SkyHanniItemTracker(
        "Archfiend Dice Profit Tracker",
        ::Data,
        { it.archfiendDiceProfitTracker },
        trackerConfig = { config.perTrackerConfig }
    ) { drawDisplay(it) }

    data class Data(
        @Expose var archfiendDiceRolls: Long = 0L,
        @Expose var highClassDiceRolls: Long = 0L,
        @Expose var archfiendDice6Count: Long = 0L,
        @Expose var archfiendDice7Count: Long = 0L,
        @Expose var highClassDice6Count: Long = 0L,
        @Expose var highClassDice7Count: Long = 0L,
        @Expose var archfiendDiceProfit: Long = 0L,
        @Expose var highClassDiceProfit: Long = 0L,
    ) : ItemTrackerData<SessionUptime.Normal>(SessionUptime.Normal::class) {
        override fun getDescription(timesGained: Long): List<String> {
            val totalRolls = archfiendDiceRolls + highClassDiceRolls
            val rolls6 = archfiendDice6Count + highClassDice6Count
            val hitRate = if (totalRolls > 0) (rolls6.toDouble() / totalRolls).formatPercentage() else "0%"
            val totalProfit = archfiendDiceProfit + highClassDiceProfit
            val profitFormatted = totalProfit.shortFormat()

            return listOf(
                "§7Rolled §e${totalRolls.addSeparators()} §7times.",
                "§7Hit rate on 6: §c$hitRate",
                "§7Total profit: §e$profitFormatted",
            )
        }

        override fun getCoinName(item: TrackedItem) = "§6Dice Roll Profit"

        override fun getCoinDescription(item: TrackedItem): List<String> {
            val totalCoins = item.totalAmount.shortFormat()
            return listOf(
                "§7Net profit from rolling dice.",
                "§7You made §6$totalCoins coins §7from rolling dice.",
            )
        }
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§e§lArchfiend Dice Profit Tracker")

        val profit = tracker.drawItems(data, { true }, this)

        val totalRolls = data.archfiendDiceRolls + data.highClassDiceRolls
        add(
            Renderable.hoverTips(
                "§7Total rolls: §e${totalRolls.addSeparators()}",
                listOf(
                    "§7Archfiend: §e${data.archfiendDiceRolls.addSeparators()}",
                    "§7High Class: §e${data.highClassDiceRolls.addSeparators()}",
                ),
            ).toSearchable(),
        )

        val totalHits = data.archfiendDice6Count + data.highClassDice6Count
        add(
            Renderable.hoverTips(
                "§7Times rolled 6: §e${totalHits.addSeparators()}",
                listOf(
                    "§7Archfiend: §e${data.archfiendDice6Count.addSeparators()}",
                    "§7High Class: §e${data.highClassDice6Count.addSeparators()}",
                ),
            ).toSearchable(),
        )

        val totalJackpots = data.archfiendDice7Count + data.highClassDice7Count
        add(
            Renderable.hoverTips(
                "§7Times rolled 7 (Jackpot): §e${totalJackpots.addSeparators()}",
                listOf(
                    "§7Archfiend: §e${data.archfiendDice7Count.addSeparators()}",
                    "§7High Class: §e${data.highClassDice7Count.addSeparators()}",
                ),
            ).toSearchable(),
        )

        val archfiendProfitFormatted = data.archfiendDiceProfit.shortFormat()
        val highClassProfitFormatted = data.highClassDiceProfit.shortFormat()
        val archfiendProfitColor = if (data.archfiendDiceProfit >= 0) "§a" else "§c"
        val highClassProfitColor = if (data.highClassDiceProfit >= 0) "§a" else "§c"

        add(
            Renderable.hoverTips(
                "§7Profit per type:",
                listOf(
                    "§7Archfiend: $archfiendProfitColor$archfiendProfitFormatted",
                    "§7High Class: $highClassProfitColor$highClassProfitFormatted",
                ),
            ).toSearchable(),
        )

        val duration = data.getTotalUptime()
        addAll(tracker.addTotalProfit(profit, totalRolls, "roll", duration, "Rolls"))

        tracker.addPriceFromButton(this)
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        diceRollChatPattern.matchMatcher(event.message) {
            lastDiceRoll = SimpleTimeMark.now()
            lastCatchTime = SimpleTimeMark.now()

            val number = group("number").toIntOrNull() ?: return@matchMatcher
            val isHighClass = group("isHighClass").isNotEmpty()
            trackDiceRoll(number, isHighClass)
        }
    }

    private fun trackDiceRoll(number: Int, isHighClass: Boolean) {
        if (number !in 1..7) return

        val diceItem = if (isHighClass) HIGH_CLASS_ARCHFIEND_DICE else ARCHFIEND_DICE
        val rollCost = if (isHighClass) HIGH_CLASS_ARCHFIEND_DICE_ROLL_COST else ARCHFIEND_DICE_ROLL_COST
        val winCost = if (isHighClass) HIGH_CLASS_ARCHFIEND_DICE_WIN_COST else ARCHFIEND_DICE_WIN_COST

        // Get current prices from auction house
        val dicePrice = diceItem.getPriceOrNull()?.toLong() ?: 0L
        val dyePrice = ARCHFIEND_DYE.getPriceOrNull()?.toLong() ?: 0L

        // Track the roll cost as a negative coin item
        tracker.addItem(NeuInternalName.SKYBLOCK_COIN, -rollCost.toInt(), command = false)

        // Update statistics
        tracker.modify { data ->
            // Get profit delta from the roll
            val profitDelta = when (number) {
                6 -> -dicePrice + winCost
                7 -> -dicePrice + dyePrice
                else -> 0L
            }

            if (isHighClass) {
                data.highClassDiceRolls++
                data.highClassDiceProfit -= rollCost
                data.highClassDiceProfit += profitDelta
                when (number) {
                    6 -> data.highClassDice6Count++
                    7 -> data.highClassDice7Count++
                }
            } else {
                data.archfiendDiceRolls++
                data.archfiendDiceProfit -= rollCost
                data.archfiendDiceProfit += profitDelta
                when (number) {
                    6 -> data.archfiendDice6Count++
                    7 -> data.archfiendDice7Count++
                }
            }
        }

        // Track items for wins
        when (number) {
            6 -> {
                // Lost dice, gained coins
                tracker.addItem(diceItem, -1, command = false)
                tracker.addItem(NeuInternalName.SKYBLOCK_COIN, winCost.toInt(), command = false)
            }
            7 -> {
                // Lost dice, gained dye
                tracker.addItem(diceItem, -1, command = false)
                tracker.addItem(ARCHFIEND_DYE, 1, command = false)
            }
        }
    }


    private var lastCatchTime = SimpleTimeMark.farPast()

    private val shouldShow: Boolean
        get() = config.enabled && lastCatchTime.passedSince() < 3.seconds

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { isEnabled() && config.enabled && shouldShow },
            onRender = {
                tracker.renderDisplay(config.position)
            },
        )
    }

    @HandleEvent
    fun onWorldChange() {
        lastCatchTime = SimpleTimeMark.farPast()
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock

    fun isDice(internalName: NeuInternalName) = internalName == ARCHFIEND_DICE || internalName == HIGH_CLASS_ARCHFIEND_DICE

    fun hasRecentDiceRoll() = lastDiceRoll.passedSince() < 500.milliseconds

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetarchfienddiceprofits") {
            description = "Resets the Archfiend Dice Profit Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }
}
