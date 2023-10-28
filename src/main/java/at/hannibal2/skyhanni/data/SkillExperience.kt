package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SkillExperience {
    // TODO USE SH-REPO
    private val actionBarPattern = ".*§3\\+.* (?<skill>.*) \\((?<overflow>.*)/(?<needed>.*)\\).*".toPattern()
    private val inventoryPattern = ".* §e(?<number>.*)§6/.*".toPattern()

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        skillExp.clear()
    }

    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        if (!LorenzUtils.inSkyBlock) return

        actionBarPattern.matchMatcher(event.message) {
            val skill = group("skill").lowercase()
            val overflow = group("overflow").formatNumber()
            val neededForNextLevel = group("needed").formatNumber()
            val nextLevel = getLevelForExpExactly(neededForNextLevel)
            val baseExp = getExpForLevel(nextLevel - 1)
            skillExp[skill] = baseExp + overflow
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Your Skills") return

        for ((_, stack) in event.inventoryItems) {
            val name = stack.name?.removeColor() ?: continue
            if (!name.contains(" ")) continue

            val lore = stack.getLore()

            var next = false
            for (line in lore) {
                if (line.contains("Progress to Level")) {
                    next = true
                    continue
                }
                if (next) {
                    val split = name.split(" ")
                    val skillName = split[0].lowercase()
                    val level = split[1].romanToDecimal()
                    val baseExp = getExpForLevel(level)
                    inventoryPattern.matchMatcher(line) {
                        val rawNumber = group("number")
                        val overflow = rawNumber.formatNumber()
                        val experience = baseExp + overflow
                        skillExp[skillName] = experience
                    }
                    next = false
                }
            }
        }
        if (skillExp.isNotEmpty()) return
    }

    companion object {
        private val skillExp = mutableMapOf<String, Long>()

        private fun getLevelForExpExactly(experience: Long): Int {
            var level = 1
            for (levelXp in levelingExp) {
                if (levelXp.toLong() == experience) {
                    return level
                }
                level++
            }

            return 0
        }

        fun getExpForNextLevel(requestedLevel: Int) = levelingExp[requestedLevel]

        fun getExpForLevel(requestedLevel: Int): Long {
            var total = 0L
            var level = 0
            for (levelXp in levelingExp) {
                total += levelXp
                level++
                if (level == requestedLevel) {
                    return total
                }
            }

            return 0
        }

        //TODO create additional event
        fun getExpForSkill(skillName: String) = skillExp[skillName.lowercase()] ?: 0

        private val levelingExp = listOf(
            50,
            125,
            200,
            300,
            500,
            750,
            1000,
            1500,
            2000,
            3500,
            5000,
            7500,
            10000,
            15000,
            20000,
            30000,
            50000,
            75000,
            100000,
            200000,
            300000,
            400000,
            500000,
            600000,
            700000,
            800000,
            900000,
            1000000,
            1100000,
            1200000,
            1300000,
            1400000,
            1500000,
            1600000,
            1700000,
            1800000,
            1900000,
            2000000,
            2100000,
            2200000,
            2300000,
            2400000,
            2500000,
            2600000,
            2750000,
            2900000,
            3100000,
            3400000,
            3700000,
            4000000,
            4300000,
            4600000,
            4900000,
            5200000,
            5500000,
            5800000,
            6100000,
            6400000,
            6700000,
            7000000
        )
    }
}

