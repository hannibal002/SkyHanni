package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.achievements.AchievementManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraft.ChatFormatting
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

    private var lastDiceRoll = SimpleTimeMark.farPast()
    private var lastDiceActivity = SimpleTimeMark.farPast()

    private val tracker = SkyHanniItemTracker(
        "Archfiend Dice Profit Tracker",
        ::Data,
        { it.archfiendDiceProfitTracker },
        trackerConfig = { config.perTrackerConfig }
    ) { drawDisplay(it) }

    data class DiceData(
        @Expose var rolls: Long = 0L,
        @Expose var sixes: Long = 0L,
        @Expose var jackpots: Long = 0L,
        @Expose var profit: Long = 0L,
    )

    data class Data(
        @Expose var archfiend: DiceData = DiceData(),
        @Expose var highClass: DiceData = DiceData(),
    ) : ItemTrackerData<SessionUptime.Normal>(SessionUptime.Normal::class) {
        override fun getDescription(timesGained: Long): List<String> {
            val totalRolls = archfiend.rolls + highClass.rolls
            val totalSixes = archfiend.sixes + highClass.sixes
            val totalJackpots = archfiend.jackpots + highClass.jackpots
            val totalSpecialRolls = totalSixes + totalJackpots

            val specialRate = if (totalRolls > 0) {
                (totalSpecialRolls.toDouble() / totalRolls).formatPercentage()
            } else {
                "0%"
            }

            val jackpotRate = if (totalRolls > 0) {
                (totalJackpots.toDouble() / totalRolls).formatPercentage()
            } else {
                "0%"
            }

            val totalProfit = archfiend.profit + highClass.profit
            val profitFormatted = totalProfit.shortFormat()

            return listOf(
                "§7Rolled §e${totalRolls.addSeparators()} §7times.",
                "§7Special roll rate: §a$specialRate",
                "§7Jackpot rate: §6$jackpotRate",
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

        val totalRolls = data.archfiend.rolls + data.highClass.rolls

        add(
            Renderable.hoverTips(
                "§7Total rolls: §e${totalRolls.addSeparators()}",
                listOf(
                    "§7Archfiend: §e${data.archfiend.rolls.addSeparators()}",
                    "§7High Class: §e${data.highClass.rolls.addSeparators()}",
                ),
            ).toSearchable(),
        )

        val totalSixes = data.archfiend.sixes + data.highClass.sixes
        val totalJackpots = data.archfiend.jackpots + data.highClass.jackpots
        val totalSpecialRolls = totalSixes + totalJackpots

        add(
            Renderable.hoverTips(
                "§7Special rolls: §e${totalSpecialRolls.addSeparators()}",
                listOf(
                    "§76s: §e${totalSixes.addSeparators()}",
                    "§7Jackpots: §6${totalJackpots.addSeparators()}",
                    "",
                    "§7Archfiend 6s: §e${data.archfiend.sixes.addSeparators()}",
                    "§7High Class 6s: §e${data.highClass.sixes.addSeparators()}",
                    "",
                    "§7Archfiend Jackpots: §6${data.archfiend.jackpots.addSeparators()}",
                    "§7High Class Jackpots: §6${data.highClass.jackpots.addSeparators()}",
                ),
            ).toSearchable(),
        )

        val totalProfit = data.archfiend.profit + data.highClass.profit
        val totalProfitFormatted = totalProfit.shortFormat()

        val archfiendProfitFormatted = data.archfiend.profit.shortFormat()
        val highClassProfitFormatted = data.highClass.profit.shortFormat()

        val archfiendProfitColor = if (data.archfiend.profit >= 0) "§a" else "§c"
        val highClassProfitColor = if (data.highClass.profit >= 0) "§a" else "§c"

        add(
            Renderable.hoverTips(
                "§7Profit per type: §e$totalProfitFormatted",
                listOf(
                    "§7Archfiend: $archfiendProfitColor$archfiendProfitFormatted",
                    "§7High Class: $highClassProfitColor$highClassProfitFormatted",
                ),
            ).toSearchable(),
        )

        val duration = data.getTotalUptime()

        addAll(
            tracker.addTotalProfit(
                profit,
                totalRolls,
                "roll",
                duration,
                "Rolls",
            ),
        )

        tracker.addPriceFromButton(this)
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return

        diceRollChatPattern.matchMatcher(event.message) {
            lastDiceRoll = SimpleTimeMark.now()
            lastDiceActivity = SimpleTimeMark.now()

            val number = group("number").toIntOrNull() ?: return@matchMatcher
            val isHighClass = group("isHighClass").isNotEmpty()

            trackDiceRoll(number, isHighClass)

            val achievement = AchievementManager.getAchievement(DICE_ACHIEVEMENT)
            AchievementManager.updateTieredAchievement(
                DICE_ACHIEVEMENT,
                achievement.data.progress + 1,
            )
        }
    }

    private fun trackDiceRoll(number: Int, isHighClass: Boolean) {
        if (number !in 1..7) return

        val diceItem = if (isHighClass) {
            HIGH_CLASS_ARCHFIEND_DICE
        } else {
            ARCHFIEND_DICE
        }

        tracker.modify { data ->
            val diceData = if (isHighClass) {
                data.highClass
            } else {
                data.archfiend
            }

            diceData.rolls++

            when (number) {
                6 -> {
                    diceData.sixes++
                    tracker.addItem(diceItem, -1, command = false)
                }

                7 -> {
                    diceData.jackpots++
                    tracker.addItem(diceItem, -1, command = false)
                    tracker.addItem(ARCHFIEND_DYE, 1, command = false)
                }
            }
        }
    }

    @HandleEvent
    fun onPurseChange(event: PurseChangeEvent) {
        if (!isEnabled()) return

        val coins = event.coins.toInt()

        when (event.reason) {
            PurseChangeCause.GAIN_DICE_ROLL_HIGH_CLASS,
            PurseChangeCause.LOSE_DICE_ROLL_COST_HIGH_CLASS -> {
                tracker.modify { data ->
                    data.highClass.profit += coins
                }

                lastDiceActivity = SimpleTimeMark.now()

                tracker.addCoins(coins, command = false)
            }

            PurseChangeCause.GAIN_DICE_ROLL_ARCHFIEND,
            PurseChangeCause.LOSE_DICE_ROLL_COST_ARCHFIEND -> {
                tracker.modify { data ->
                    data.archfiend.profit += coins
                }

                lastDiceActivity = SimpleTimeMark.now()

                tracker.addCoins(coins, command = false)
            }

            else -> return
        }
    }

    private val shouldShow: Boolean
        get() = config.enabled && lastDiceActivity.passedSince() < 3.seconds

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
        lastDiceActivity = SimpleTimeMark.farPast()
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

    private const val DICE_ACHIEVEMENT = "100 dice rolls"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Professional Gambler".asComponent(),
            componentBuilder {
                append("Spin 100 dice")
                append(" I doubt you make money from this...") {
                    withColor(ChatFormatting.DARK_GRAY)
                }
            },
            7f,
            false,
            listOf(100),
        )
        event.register(achievement, DICE_ACHIEVEMENT)
    }
}
