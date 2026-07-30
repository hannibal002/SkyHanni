package at.hannibal2.skyhanni.features.skillprogress

import at.hannibal2.skyhanni.api.SkillApi
import at.hannibal2.skyhanni.api.SkillApi.activeSkill
import at.hannibal2.skyhanni.api.SkillApi.exactLevelingMap
import at.hannibal2.skyhanni.api.SkillApi.levelingMap
import com.google.common.base.Splitter

object SkillUtil {

    val SPACE_SPLITTER = Splitter.on("  ").omitEmptyStrings().trimResults()
    private const val XP_NEEDED_FOR_60 = 111_672_425L

    fun getSkillInfo(skill: SkillType): SkillApi.SkillInfo? {
        return SkillApi.storage?.get(skill)
    }

    fun xpRequiredForLevel(desiredLevel: Int): Long {
        var totalXP = 0L
        val maxLevel = 60

        if (desiredLevel <= maxLevel) {
            for (level in 1..desiredLevel) {
                totalXP += levelingMap[level]?.toLong() ?: 0L
            }
        } else {
            val xpNeeded = XP_NEEDED_FOR_60

            totalXP += xpNeeded

            var level = 60
            var xpForNext = 7_000_000L + 600_000L
            var slope = 600_000L

            while (level < desiredLevel) {
                totalXP += xpForNext
                level++
                xpForNext += slope

                if (level % 10 == 0) slope *= 2
            }
        }

        return totalXP
    }

    fun getLevelExact(neededXP: Long): Int {
        return exactLevelingMap.getOrDefault(neededXP.toInt(), activeSkill?.maxLevel ?: 60)
    }

    fun calculateLevelXP(level: Int): Double {
        return SkillApi.levelArray.asSequence().take(level + 1).sumOf { it.toDouble() }
    }

    /**
     * The returned level is uncapped, so it keeps counting past [maxSkillCap]. Only
     * [SkillLevel.overflowXP] is relative to the cap; callers that want the capped level have to
     * coerce it themselves.
     */
    fun calculateSkillLevel(currentXP: Long, maxSkillCap: Int): SkillLevel {
        var xpCurrent = currentXP
        var level = 0

        // up to level 60 the cost of every single level is listed in the leveling table
        while (level < 60) {
            val xpForNextLevel = levelingMap[level + 1]?.toLong() ?: break
            if (xpCurrent < xpForNextLevel) break
            xpCurrent -= xpForNextLevel
            level++
        }

        var xpForNext = levelingMap[level + 1]?.toLong() ?: 0L

        // past level 60 the cost grows by a slope that doubles every tenth level instead
        if (level >= 60) {
            var slope = 600_000L
            xpForNext = 7_000_000L + slope
            while (xpCurrent >= xpForNext) {
                level++
                xpCurrent -= xpForNext
                xpForNext += slope
                if (level % 10 == 0) slope *= 2
            }
        }

        val xpForCap = xpRequiredForLevel(maxSkillCap.coerceAtMost(60))
        val overflowXP = (currentXP - xpForCap).coerceAtLeast(0L)

        return SkillLevel(level, xpCurrent, xpForNext, overflowXP)
    }

}
