package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

@SkyHanniModule
object FannCost {

    private val config get() = SkyHanniMod.feature.inventory.fannCost
    private val showCoins get() = config.coinsPerXP
    private val showBits get() = config.xpPerBit
    private var trainingMode: TrainingMode = TrainingMode.DAY_COUNT

    private val patternGroup = RepoPattern.group("fann.inventory")

    /**
     * REGEX-TEST: Training Slot 1
     * REGEX-TEST: Training Slot 2
     * REGEX-TEST: Training Slot 3
     */
    private val trainingSlotInventoryPattern by patternGroup.pattern(
        "training",
        "Training Slot [1-3]",
    )

    /**
     * REGEX-TEST: §aBegin Training
     */
    private val anvilPattern by patternGroup.pattern(
        "anvil",
        "§aBegin Training",
    )

    /**
     * REGEX-TEST: Will earn a total of 1,000,000 EXP.
     * REGEX-TEST: Will earn a total of 2 EXP
     * REGEX-TEST: Will earn a total of 1,000 EXP
     */
    private val expEarnedPattern by patternGroup.pattern(
        "exp.total",
        "Will earn a total of (\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?|\\d+\\.?\\d*) EXP\\.?",
    )

    /**
     * REGEX-TEST: EXP Per Day: 1,000
     * REGEX-TEST: EXP Per Day: 1,230,000 (+3.4%)
     * REGEX-TEST: EXP Per Day: 1,623,000 (+9.1%)
     * REGEX-TEST: EXP Per Day: 1
     */
    private val dailyExpPattern by patternGroup.pattern(
        "exp.daily",
        "EXP Per Day: ([\\d,]+)(?: \\(\\+\\d{1,2}(\\.\\d{1,2})?%\\))?",
    )

    /**
     * REGEX-TEST: Will take: 1d 0h 0m 0s
     * REGEX-TEST: Will take: 3d 11h 10m 35s
     * REGEX-TEST: Will take: 0d 0h 0m 1s
     * REGEX-TEST: Will take: 493d 19h 49m 59s
     */
    private val durationPattern by patternGroup.pattern(
        "training.duration.pattern",
        "Will take: (?<day>\\d+)d (?<hr>\\d{1,2})h (?<min>\\d{1,2})m (?<sec>\\d{1,2})s",
    )

    /**
     * REGEX-TEST: 6,749,742 Coins
     * REGEX-TEST: 13,492,398.8 Coins
     * REGEX-TEST: 1,000,000.3 Coins (1% off)
     * REGEX-TEST: 12,345,678 Coins (5% off)
     */
    private val coinsPattern by patternGroup.pattern(
        "coin",
        "(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?|\\d+\\.?\\d*) Coins(?: \\([1-5]% off\\))?",
    )

    /**
     * REGEX-TEST: 5,024.3 Bits
     * REGEX-TEST: 1,000 Bits
     * REGEX-TEST: 139 Bits
     */
    private val bitsPattern by patternGroup.pattern(
        "bits",
        "(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?|\\d+\\.?\\d*) Bits",
    )

    /**
     * REGEX-TEST: Desired Level: 200
     * REGEX-TEST: Desired Level: 100
     * REGEX-TEST: Desired Level: 99
     * REGEX-TEST: Desired Level: 4
     */
    private val desiredLevelPatter by patternGroup.pattern(
        "slot24.name.level",
        "Desired Level: (200|1?[0-9]?[0-9])",
    )

    /**
     * REGEX-TEST: User Input
     */
    private val userInputPattern by patternGroup.pattern(
        "slot24.name.input",
        "User Input",
    )

    /**
     * REGEX-TEST: Type: Free
     * REGEX-TEST: Type: Light
     * REGEX-TEST: Type: Moderate
     * REGEX-TEST: Type: Expert
     * REGEX-TEST: Type: Ultra
     * REGEX-TEST: Type: Turbo!
     */
    private val trainingTypePattern by patternGroup.pattern(
        "training.type",
        "Type: (Free|Light|Moderate|Expert|Ultra|Turbo!)",
    )

    @SubscribeEvent
    fun onFannAnvilTooltip(event: LorenzToolTipEvent) {
        if (!trainingSlotInventoryPattern.matches(InventoryUtils.openInventoryName())) return
        if (!anvilPattern.matches(event.itemStack.displayName)) return
        val tooltip = event.toolTip

        val trainingType = tooltip.getTrainingType() ?: return
        if (trainingType == TrainingType.FREE) return
        if (!showCoins || !showBits) return

        when (trainingMode) {
            TrainingMode.DAY_COUNT -> {
                val totalExp = tooltip.getExpEarned() ?: return
                val coinPerExp = tooltip.getCoins() / totalExp
                val xpPerBit = totalExp / tooltip.getBits()

                tooltip.insertLineAfter(coinsPattern, "§6➜Coins/XP: ${coinPerExp.roundTo(2)}")
                tooltip.insertLineAfter(bitsPattern, "§b➜XP/Bit: ${xpPerBit.roundTo(2)}")

            }

            TrainingMode.UNTIL_LEVEL -> {
                val dailyExp = tooltip.getDailyExp() ?: return
                val duration = tooltip.getDuration() ?: return
                val totalExp = dailyExp * duration
                val coinPerExp = tooltip.getCoins() / totalExp
                val xpPerBit = totalExp / tooltip.getBits()
                tooltip.insertLineAfter(coinsPattern, "§6➜Coins/XP: ${coinPerExp.roundTo(2)}")
                tooltip.insertLineAfter(bitsPattern, "§b➜XP/Bit: ${xpPerBit.roundTo(2)}")

            }
        }
    }

    @SubscribeEvent
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!trainingSlotInventoryPattern.matches(event.inventoryName.removeColor())) return
        val slot24 = event.inventoryItems[24] ?: return

        val name = slot24.displayName.removeColor()
        if (desiredLevelPatter.matches(name)) {
            trainingMode = TrainingMode.UNTIL_LEVEL
        } else if (userInputPattern.matches(name)) {
            trainingMode = TrainingMode.DAY_COUNT
        }

    }

    private fun MutableList<String>.insertLineAfter(pattern: Pattern, content: String) {
        val iter = this.listIterator()
        while (iter.hasNext()) {
            val line = iter.next().removeColor()
            if (pattern.matcher(line).find()) {
                iter.add(content)
            }
        }
    }

    // To avoid duplicate code
    private fun <T> Pattern.read(lore: List<String>, func: (String) -> T): T? {
        for (line in lore) {
            val linePlain = line.removeColor()
            val matcher = matcher(linePlain)
            if (matcher.find()) {
                val res = matcher.group(1)
                return func(res)
            }
        }
        return null
    }

    private fun List<String>.getCoins(): Double {
        return coinsPattern.read(this) { it.formatDouble() } ?: 0.0
    }

    // In case of Bits not found, return 1 so the division is not by zero
    private fun List<String>.getBits(): Double {
        return bitsPattern.read(this) { it.formatDouble() } ?: 1.0
    }

    private fun List<String>.getExpEarned(): Double? {
        return expEarnedPattern.read(this) { it.formatDouble() }
    }

    private fun List<String>.getDailyExp(): Double? {
        return dailyExpPattern.read(this) { it.formatDouble() }
    }

    private fun List<String>.getTrainingType(): TrainingType? {
        return trainingTypePattern.read(this) { typestr ->
            TrainingType.entries.firstOrNull { it.type == typestr }
        }
    }

    private fun List<String>.getDuration(): Double? {
        for (line in this) {
            val linePlain = line.removeColor()
            val matcher = durationPattern.matcher(linePlain)
            if (matcher.find()) {
                // Extract the named groups and convert them to integers
                val days = matcher.groupOrNull("day")?.toInt() ?: 0
                val hours = matcher.groupOrNull("hr")?.toInt() ?: 0
                val minutes = matcher.groupOrNull("min")?.toInt() ?: 0
                val seconds = matcher.groupOrNull("sec")?.toInt() ?: 0

                // Calculate the total duration in days
                val totalDays = days + hours / 24.0 + minutes / 1440.0 + seconds / 86400.0

                // Return the total duration as a Double representing days
                return totalDays
            }
        }
        return null  // Return null if no valid duration is found
    }

    private enum class TrainingMode {
        DAY_COUNT,
        UNTIL_LEVEL,
    }

    private enum class TrainingType(val type: String) {
        FREE("Free"),
        LIGHT("Light"),
        MODERATE("Moderate"),
        EXPERT("Expert"),
        ULTRA("Ultra"),
        TURBO("Turbo!"),
    }
}
