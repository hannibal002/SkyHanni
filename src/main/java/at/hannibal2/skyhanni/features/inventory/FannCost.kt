package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
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


    private val trainingSlotInventoryPattern by patternGroup.pattern(
        "training",
        "Training Slot [1-3]",
    )
    private val anvilPattern by patternGroup.pattern(
        "anvil",
        "§aBegin Training",
    )
    private val expEarnedPattern by patternGroup.pattern(
        "exp.total",
        "Will earn a total of (\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?|\\d+\\.?\\d*) EXP\\.?",
    )
    private val dailyExpPattern by patternGroup.pattern(
        "exp.daily",
        "EXP Per Day: ([\\d,]+)(?: \\(\\+\\d{1,2}(\\.\\d{1,2})?%\\))?",
    )
    private val durationPattern by patternGroup.pattern(
        "training.duration.pattern",
        "Will take: (?<day>\\d+)d (?<hr>\\d{1,2})h (?<min>\\d{1,2})m (?<sec>\\d{1,2})s",
    )
    private val coinsPattern by patternGroup.pattern(
        "coin",
        "(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?|\\d+\\.?\\d*) Coins(?: \\([1-5]% off\\))?",
    )
    private val bitsPattern by patternGroup.pattern(
        "bits",
        "(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?|\\d+\\.?\\d*) Bits",
    )
    private val desiredLevelPatter by patternGroup.pattern(
        "slot24.name.level",
        "Desired Level: (200|1?[0-9]?[0-9])",
    )
    private val userInputPattern by patternGroup.pattern(
        "slot24.name.input",
        "User Input",
    )
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

        when (trainingMode) {
            TrainingMode.DAY_COUNT -> {
                if (trainingType == TrainingType.FREE) return
                if (!showCoins || !showBits) return

                val totalExp = tooltip.getExpEarned() ?: return
                val coinPerExp = tooltip.getCoins() / totalExp
                val xpPerBit = totalExp / tooltip.getBits()

                tooltip.insertLineAfter(coinsPattern, "   §6Coins/XP: ${coinPerExp.roundTo(2)}")
                tooltip.insertLineAfter(bitsPattern, "   §6XP/Bit: ${xpPerBit.roundTo(2)}")

            }

            TrainingMode.UNTIL_LEVEL -> {
                if (trainingType == TrainingType.FREE) return
                if (!showCoins || !showBits) return

                val dailyExp = tooltip.getDailyExp() ?: return
                val duration = tooltip.getDuration() ?: return
                val totalExp = dailyExp * duration
                val coinPerExp = tooltip.getCoins() / totalExp
                val xpPerBit = totalExp / tooltip.getBits()
                tooltip.insertLineAfter(coinsPattern, "   §6Coins/XP: ${coinPerExp.roundTo(2)}")
                tooltip.insertLineAfter(bitsPattern, "   §6XP/Bit: ${xpPerBit.roundTo(2)}")

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
        return coinsPattern.read(this) { it._toDouble() } ?: 0.0
    }

    // In case of Bits not found, return 1 so the division is not by zero
    private fun List<String>.getBits(): Double {
        return bitsPattern.read(this) { it._toDouble() } ?: 1.0
    }

    private fun List<String>.getExpEarned(): Double? {
        return expEarnedPattern.read(this) { it._toDouble() }
    }

    private fun List<String>.getDailyExp(): Double? {
        return dailyExpPattern.read(this) { it._toDouble() }
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

    private fun String._toDouble(): Double {
        return this.replace(",", "").toDouble()
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
