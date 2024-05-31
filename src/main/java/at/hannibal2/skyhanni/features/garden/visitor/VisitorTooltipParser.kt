package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.config.features.garden.GardenConfig
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.RegexUtils.find
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

class VisitorTooltipParser {
    class ParsedTooltip(
        val itemsNeeded: MutableMap<String, Int>,
        val rewards: MutableMap<String, Int>,
        val config: GardenConfig,
    )

    enum class ParsingSection {
        ITEMS_NEEDED,
        REWARDS
    }

    companion object {

        private val patternGroup = RepoPattern.group("visitortooltipparser")
        private val rewardsPattern by patternGroup.pattern(
            "rewards",
            "Rewards:"
        )

        fun parse(lore: List<String>, config: GardenConfig?): ParsedTooltip {
            var section = ParsingSection.ITEMS_NEEDED
            val parsedData = ParsedTooltip(mutableMapOf(), mutableMapOf(), config ?: GardenConfig())
            for (line in lore) {
                if (line.isBlank()) continue
                val isRewardSection = rewardsPattern.find(line)
                if (isRewardSection) {
                    section = ParsingSection.REWARDS
                    continue
                }

                val (itemName, amount) = ItemUtils.readItemAmount(line) ?: continue

                when (section) {
                    ParsingSection.ITEMS_NEEDED -> parsedData.itemsNeeded[itemName] = amount
                    ParsingSection.REWARDS -> parsedData.rewards[itemName] = amount
                }
            }

            return parsedData
        }
    }
}
