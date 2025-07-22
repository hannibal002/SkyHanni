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

    fun calculateXPForCurrentLevel(level: Int): Long {
        return SkillApi.levelArray.getOrNull(level)?.toLong() ?: 4000000L
    }

    fun calculateXPToNextLevel(currentLevel: Int): Long {
        val xpForCurrentLevel = SkillApi.levelArray.getOrNull(currentLevel)?.toLong() ?: 4000000L
        val xpForNextLevel = SkillApi.levelArray.getOrNull(currentLevel + 1)?.toLong() ?: 4300000L

        return xpForNextLevel - xpForCurrentLevel
    }

    fun calculateSkillLevel(currentXP: Long, maxSkillCap: Int): SkillLevel {
        var xpCurrent = currentXP
        var level = 0
        val maxLevel = maxSkillCap.coerceAtMost(60)

        while (level < maxLevel && xpCurrent >= (levelingMap[level + 1]?.toLong() ?: Long.MAX_VALUE)) {
            val xpForNextLevel = levelingMap[level + 1]?.toLong() ?: Long.MAX_VALUE
            xpCurrent -= xpForNextLevel
            level++
        }

        var xpForNext = levelingMap[level + 1]?.toLong() ?: 0L
        var overflowXP = 0L

        if (level >= maxLevel) {
            val xpNeeded = xpRequiredForLevel(maxLevel)

            if (currentXP >= xpNeeded) {
                overflowXP = currentXP - xpNeeded

                xpCurrent = overflowXP
                var slope = calculateXPToNextLevel(maxLevel)
                var xpForCurr = calculateXPForCurrentLevel(maxLevel) + slope

                while (xpCurrent >= xpForCurr && level < 60) {
                    level++
                    xpCurrent -= xpForCurr
                    slope = calculateXPToNextLevel(level)
                    xpForCurr += slope
                }

                if (level >= 60) {
                    slope = 600000L
                    xpForCurr = 7000000L + slope
                    while (xpCurrent >= xpForCurr) {
                        level++
                        xpCurrent -= xpForCurr
                        xpForCurr += slope
                        if (level % 10 == 0) slope *= 2
                    }
                }

                xpForNext = xpForCurr
            }
        }

        return SkillLevel(level, xpCurrent, xpForNext, overflowXP)
    }

}
