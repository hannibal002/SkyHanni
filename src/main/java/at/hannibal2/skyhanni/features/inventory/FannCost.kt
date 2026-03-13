package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.Resettable
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.add
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.insertLineAfter
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit

@SkyHanniModule
object FannCost {

    enum class FannTrainingMode(private val displayName: String) {
        DAY_COUNT("Amount of Days"),
        UNTIL_LEVEL("Until Specific Level"),
        ;

        override fun toString() = displayName
    }

    enum class FannTrainingType(private val displayName: String) {
        FREE("Free"),
        LIGHT("Light"),
        MODERATE("Moderate"),
        EXPERT("Expert"),
        ULTRA("Ultra"),
        TURBO("Turbo!"),
        ;

        override fun toString() = displayName
    }

    data class FannData(
        var trainingMode: FannTrainingMode = FannTrainingMode.DAY_COUNT,
        var trainingType: FannTrainingType = FannTrainingType.FREE,
        var coinCost: Double = 0.0,
        var bitCost: Double = 0.0,
        var expEarned: Double? = null,
        var expDaily: Double? = null,
        var duration: Duration = Duration.ZERO,
    ) : Resettable

    private const val TRAINING_DURATION_SLOT_NUM = 15
    private const val BEGIN_TRAINING_SLOT_NUM = 22
    private const val USER_INPUT_SLOT_NUM = 24
    private const val TRAINING_TYPE_SLOT_NUM = 33

    private val config get() = SkyHanniMod.feature.inventory.fannCost
    private val patternGroup = RepoPattern.group("fann.inventory")
    private val currentFannData: FannData = FannData()
    private val generatedTooltips: MutableMap<Pattern, String> = mutableMapOf()
    private val trainingSlotInventoryDetector = InventoryDetector { name -> name.contains("Training Slot") }

    private var lastStartTrainingLoreHash: Int = 0

    /**
     * @regexTest §7§8Will earn a total of 55,000 EXP
     * @regexTest §7§8Will earn a total of 550,000 EXP
     */
    private val expEarnedPattern by patternGroup.pattern(
        "exp.total",
        "(?:§.)+Will earn a total of (?<expEarned>.*) EXP\\.?",
    )

    /**
     * @regexTest §7EXP Per Day: §b1,000
     * @regexTest §7EXP Per Day: §b1,230,000 §8(+3.4%)
     * @regexTest §7EXP Per Day: §b1,623,000 §8(+9.1%)
     * @regexTest §7EXP Per Day: §b55,000 §8(+10%)
     */
    private val expDailyPattern by patternGroup.pattern(
        "exp.daily",
        "(?:§.)+EXP Per Day: (?:§.)+(?<expDaily>\\d[\\d,]*)(?: (?:§.)+\\(\\+(?<extraPercent>[\\d.]+)%\\))?",
    )

    /**
     * @regexTest §7Will take: §e0d 0h 10m 29s
     * @regexTest §7Will take: §e44d 6h 14m 40s
     * @regexTest §7Will take: §e442d 14h 26m 31s
     */
    private val durationPattern by patternGroup.pattern(
        "training.duration.pattern",
        "(?:§.)+Will take: (?:§.)+(?<time>.*)",
    )

    /**
     * @regexTest §622,795.5 Coins §8(5% off)
     * @regexTest §613,492,398.8 Coins
     * @regexTest §61,000,000.3 Coins §8(1% off)
     * @regexTest §612,345,678 Coins §8(5% off)
     */
    private val coinsPattern by patternGroup.pattern(
        "coin",
        "(?:§.)+(?<coins>[^ ]+) Coins(?: (?:§.)+\\((?<percentOff>[\\d.]+)% off\\))?",
    )

    /**
     * @regexTest §b5,024.3 Bits
     * @regexTest §b1,000 Bits
     * @regexTest §b139 Bits
     */
    private val bitsPattern by patternGroup.pattern(
        "bits",
        "(?:§.)+(?<bits>[^ ]+) Bits",
    )


    /**
     * @regexTest §b▶ Amount of Days
     * @regexTest §b▶ Until Specific Level
     */
    private val trainingModeLorePattern by patternGroup.pattern(
        "lore.training-mode",
        "(?:§.)+▶ (?<selection>.*)"
    )

    /**
     * @regexTest §f▶ Free
     * @regexTest §a▶ Light
     * @regexTest §9▶ Moderate
     * @regexTest §5▶ Expert
     * @regexTest §6▶ Ultra
     * @regexTest §d▶ Turbo!
     */
    private val trainingTypeLorePattern by patternGroup.pattern(
        "lore.training-type",
        "(?:§.)+▶ (?<type>.*)",
    )

    /**
     * @regexTest §b▶ 1 Day
     * @regexTest §b▶ 5 Days
     * @regexTest §b▶ 20 Days
     */
    private val dayUserInputLorePattern by patternGroup.pattern(
        "lore.day-user-input",
        "(?:§.)+▶ (?<days>\\d+) Days?"
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onFannAnvilTooltip(event: ToolTipEvent) {
        if (!config.coinsPerXP && !config.xpPerBit) return
        if (currentFannData.trainingType == FannTrainingType.FREE) return

        generatedTooltips.forEach { (pattern, insertionTip) ->
            event.toolTip.insertLineAfter(pattern, insertionTip)
        }
    }

    private fun FannData.generateNewTooltips(): List<Pair<Pattern, String>> {
        val expGained = when (trainingMode) {
            FannTrainingMode.DAY_COUNT -> expEarned
            FannTrainingMode.UNTIL_LEVEL -> expDaily?.times(duration.toDouble(DurationUnit.DAYS))
        } ?: return emptyList()

        val coinPerExp = coinCost / expGained
        val xpPerBit = expGained / max(1.0, bitCost)

        return listOf(
            coinsPattern to "§7 = §6${coinPerExp.roundTo(2)} Coins§7/§bXP",
            bitsPattern to "§7 = §b${xpPerBit.roundTo(2)} XP§7/§bBit"
        )
    }

    private fun List<String>.getGroupDouble(
        pattern: Pattern,
        groupName: String
    ): Double? = pattern.firstMatcher(this) {
        group(groupName).formatDouble()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!trainingSlotInventoryDetector.isInside()) {
            currentFannData.reset()
            generatedTooltips.clear()
            return
        }

        val beginTrainingLore = event.inventoryItems[BEGIN_TRAINING_SLOT_NUM]?.getLore()?.takeIf {
            it.isNotEmpty() && it.hashCode() != lastStartTrainingLoreHash
        } ?: return
        lastStartTrainingLoreHash = beginTrainingLore.hashCode()
        val coins = beginTrainingLore.getGroupDouble(coinsPattern, "coins") ?: 0.0
        val bits = beginTrainingLore.getGroupDouble(bitsPattern, "bits") ?: 0.0
        val expEarned = beginTrainingLore.getGroupDouble(expEarnedPattern, "expEarned")
        val expDaily = beginTrainingLore.getGroupDouble(expDailyPattern, "expDaily")
        val duration = durationPattern.firstMatcher(beginTrainingLore) {
            TimeUtils.getDuration(group("time"))
        } ?: dayUserInputLorePattern.firstMatcher(
            event.inventoryItems[USER_INPUT_SLOT_NUM]?.getLore().orEmpty()
        ) { group("days").formatInt().days } ?: Duration.ZERO

        val trainingDurationSlot = event.inventoryItems[TRAINING_DURATION_SLOT_NUM] ?: return
        val trainingMode = trainingModeLorePattern.firstMatcher(trainingDurationSlot.getLore()) {
            FannTrainingMode.entries.firstOrNull { it.toString() == group("selection") }
        } ?: return

        val trainingTypeSlot = event.inventoryItems[TRAINING_TYPE_SLOT_NUM] ?: return
        val trainingType = trainingTypeLorePattern.firstMatcher(trainingTypeSlot.getLore()) {
            FannTrainingType.entries.firstOrNull { it.toString() == group("type") }
        } ?: return

        currentFannData.apply {
            this.trainingMode = trainingMode
            this.trainingType = trainingType
            this.coinCost = coins
            this.bitCost = bits
            this.expEarned = expEarned
            this.expDaily = expDaily
            this.duration = duration
        }
        generatedTooltips.clear()
        currentFannData.generateNewTooltips().forEach {
            generatedTooltips.add(it)
        }
    }
}
