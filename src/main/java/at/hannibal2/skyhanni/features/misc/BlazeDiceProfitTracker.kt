package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.PurseApi
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
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
object BlazeDiceProfitTracker {

    private val config get() = SkyHanniMod.feature.misc.blazeDiceProfitTracker

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

    private var lastDiceActivity = SimpleTimeMark.farPast()
    private var holdingDice = false

    private val tracker = SkyHanniItemTracker(
        "Archfiend Dice Profit Tracker",
        ::Data,
        { it.archfiendDiceProfitTracker },
        trackerConfig = { config.perTrackerConfig }
    ) { drawDisplay(it) }

    data class DiceData(
        @Expose var rolls: Long = 0L,
        @Expose var jackpots: Long = 0L,
        @Expose var dyeCount: Long = 0L,
        @Expose var rollCost: Long = 0L,
    )

    data class Data(
        @Expose var archfiend: DiceData = DiceData(),
        @Expose var highClass: DiceData = DiceData(),
    ) : ItemTrackerData<SessionUptime.Normal>(SessionUptime.Normal::class) {
        override fun getDescription(timesGained: Long): List<String> {
            val totalRolls = archfiend.rolls + highClass.rolls
            val totalJackpots = archfiend.jackpots + highClass.jackpots

            val jackpotRate = if (totalRolls > 0) {
                (totalJackpots.toDouble() / totalRolls).formatPercentage()
            } else {
                "0%"
            }

            return listOf(
                "§7Jackpot rate: §a$jackpotRate",
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

        var profit = tracker.drawItems(data, { true }, this)

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

        val totalJackpots = data.archfiend.jackpots + data.highClass.jackpots

        if (totalJackpots > 0) {
            add(
                Renderable.hoverTips(
                    "§7Jackpots: §e${totalJackpots.addSeparators()}",
                    listOf(
                        "§7Archfiend: §e${data.archfiend.jackpots.addSeparators()}",
                        "§7High Class: §e${data.highClass.jackpots.addSeparators()}",
                    ),
                ).toSearchable(),
            )
        }

        val totalDyes = data.archfiend.dyeCount + data.highClass.dyeCount

        if (totalDyes > 0) {
            add(
                Renderable.hoverTips(
                    "§7Dye drops (7s): §6${totalDyes.addSeparators()}",
                    listOf(
                        "§7Archfiend Dyes: §6${data.archfiend.dyeCount.addSeparators()}",
                        "§7High Class Dyes: §6${data.highClass.dyeCount.addSeparators()}",
                    ),
                ).toSearchable(),
            )
        }

        val totalRollCost = data.archfiend.rollCost + data.highClass.rollCost
        val totalRollCostFormatted = totalRollCost.shortFormat()
        add(
            Renderable.hoverTips(
                "§7Dice Roll Costs: §c$totalRollCostFormatted",
                listOf(
                    "§7Archfiend: §c${data.archfiend.rollCost.shortFormat()}",
                    "§7High Class: §c${data.highClass.rollCost.shortFormat()}",
                ),
            ).toSearchable(),
        )
        profit += totalRollCost

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

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        holdingDice = isDice(event.newItem)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        diceRollChatPattern.matchMatcher(event.message) {
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

        val diceItem = if (isHighClass) HIGH_CLASS_ARCHFIEND_DICE else ARCHFIEND_DICE

        tracker.modify { data ->
            val diceData = if (isHighClass) data.highClass else data.archfiend
            diceData.rolls++

            when (number) {
                6 -> {
                    diceData.jackpots++
                    data.addItem(diceItem, -1, command = false)
                }

                7 -> {
                    diceData.dyeCount++
                    data.addItem(diceItem, -1, command = false)
                    data.addItem(ARCHFIEND_DYE, 1, command = false)
                }
            }
        }
    }

    @HandleEvent
    fun onPurseChange(event: PurseChangeEvent) {
        val coins = event.coins.toInt()
        when (event.reason) {
            PurseChangeCause.LOSE_DICE_ROLL_COST_ARCHFIEND -> {
                lastDiceActivity = SimpleTimeMark.now()
                tracker.modify { data ->
                    data.archfiend.rollCost += coins.toLong()
                }
            }
            PurseChangeCause.LOSE_DICE_ROLL_COST_HIGHCLASS -> {
                lastDiceActivity = SimpleTimeMark.now()
                tracker.modify { data ->
                    data.highClass.rollCost += coins.toLong()
                }
            }
            PurseChangeCause.GAIN_DICE_ROLL_ARCHFIEND,
            PurseChangeCause.GAIN_DICE_ROLL_HIGHCLASS,
            -> {
                lastDiceActivity = SimpleTimeMark.now()
                tracker.addCoins(coins, command = false)
            }
            else -> return
        }
    }

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { isEnabled() && (holdingDice || lastDiceActivity.passedSince() < 10.seconds) },
            onRender = {
                tracker.renderDisplay(config.position)
            },
        )
    }

    @HandleEvent
    fun onWorldChange() {
        lastDiceActivity = SimpleTimeMark.farPast()
    }

    private fun isEnabled() = config.enabled

    fun isDice(internalName: NeuInternalName) = internalName == ARCHFIEND_DICE || internalName == HIGH_CLASS_ARCHFIEND_DICE

    fun hasRecentDiceRoll() = lastDiceActivity.passedSince() < 500.milliseconds

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetarchfienddiceprofits") {
            description = "Resets the Archfiend Dice Profit Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
        event.registerBrigadier("shrollblazedice") {
            description = "Manually track a dice roll. Usage: /shrolldice <arch|highclass> <number>"
            category = CommandCategory.DEVELOPER_TEST
            arg("type", BrigadierArguments.string(), listOf("archfiend", "highclass")) { type ->
                arg("number", BrigadierArguments.integer(1, 7), (1..7).map { it.toString() }) { number ->
                    callback {
                        val typeStr = getArg(type)
                        val isHighClass = when (typeStr.lowercase()) {
                            "archfiend", "arch_fiend", "arch" -> false
                            "highclass", "high_class", "high" -> true
                            else -> {
                                SkyHanniMod.logger.warn("Invalid dice type: $typeStr")
                                return@callback
                            }
                        }

                        val num = getArg(number)
                        trackDiceRoll(num, isHighClass)

                        if (isHighClass) {
                            // high class roll cost
                            onPurseChange(
                                PurseChangeEvent(
                                    -PurseApi.HIGH_CLASS_COST,
                                    PurseApi.currentPurse,
                                    PurseChangeCause.LOSE_DICE_ROLL_COST_HIGHCLASS
                                )
                            )
                            if (num == 6) {
                                onPurseChange(
                                    PurseChangeEvent(
                                        PurseApi.HIGH_CLASS_PROFIT,
                                        PurseApi.currentPurse,
                                        PurseChangeCause.GAIN_DICE_ROLL_HIGHCLASS
                                    )
                                )
                            }
                        } else {
                            // archfiend roll cost
                            onPurseChange(
                                PurseChangeEvent(
                                    -PurseApi.ARCHFIEND_COST,
                                    PurseApi.currentPurse,
                                    PurseChangeCause.LOSE_DICE_ROLL_COST_ARCHFIEND
                                )
                            )
                            if (num == 6) {
                                onPurseChange(
                                    PurseChangeEvent(
                                        -PurseApi.ARCHFIEND_PROFIT,
                                        PurseApi.currentPurse,
                                        PurseChangeCause.GAIN_DICE_ROLL_ARCHFIEND
                                    )
                                )
                            }
                        }
                    }
                }
            }
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
