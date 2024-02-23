package at.hannibal2.skyhanni.features.skillprogress

import at.hannibal2.skyhanni.api.SkillAPI
import at.hannibal2.skyhanni.api.SkillAPI.activeSkill
import at.hannibal2.skyhanni.api.SkillAPI.exactLevelingMap
import at.hannibal2.skyhanni.api.SkillAPI.excludedSkills
import at.hannibal2.skyhanni.api.SkillAPI.levelingMap
import at.hannibal2.skyhanni.utils.Quad
import com.google.common.base.Splitter
import com.google.gson.JsonArray
import io.github.moulberry.notenoughupdates.util.Constants
import io.github.moulberry.notenoughupdates.util.Utils

object SkillUtil {

    val SPACE_SPLITTER = Splitter.on("  ").omitEmptyStrings().trimResults()
    const val XP_NEEDED_FOR_60 = 111_672_425L

    fun getSkillInfo(skill: SkillType): SkillAPI.SkillInfo? {
        return SkillAPI.storage?.get(skill)
    }

    fun getSkillInfo(currentLevel: Int, currentXp: Long, neededXp: Long, totalXp: Long): Quad<Int, Long, Long, Long> {
        return if (currentLevel == 50 && neededXp == 0L)
            calculateOverFlow50(currentXp)
        else if (currentLevel >= 60)
            calculateOverFlow(currentXp)
        else
            Quad(currentLevel, currentXp, neededXp, totalXp)
    }

    /**
     * @author Soopyboo32
     */
    fun calculateOverFlow(currentXp: Long): Quad<Int, Long, Long, Long> {
        var xpCurrent = currentXp
        var slope = 600000L
        var xpForCurr = 7000000 + slope
        var level = 60
        var total = 0L
        while (xpCurrent > xpForCurr) {
            level++
            xpCurrent -= xpForCurr
            total += xpForCurr
            xpForCurr += slope
            if (level % 10 == 0) slope *= 2
        }
        total += xpCurrent
        return Quad(level, xpCurrent, xpForCurr, total)
    }

    /**
     * Calculate overflow starting at level 50
     */
    private fun calculateOverFlow50(currentXp: Long): Quad<Int, Long, Long, Long> {
        var xpCurrent = currentXp
        var level = 50
        var total = 0L
        var slope = 300000L
        var xpForCurr = 4000000 + slope

        while (level < 60 && xpCurrent > xpForCurr) {
            level++
            xpCurrent -= xpForCurr
            total += xpForCurr
            xpForCurr += slope
            if (level % 10 == 0) slope *= 2
        }

        if (level >= 60) {
            slope = 600000L
            xpForCurr = 7000000 + slope
        }

        while (xpCurrent > xpForCurr) {
            level++
            xpCurrent -= xpForCurr
            total += xpForCurr
            xpForCurr += slope
            if (level % 10 == 0) slope *= 2
        }
        total += xpCurrent

        return Quad(level, xpCurrent, xpForCurr, total)
    }

    fun xpRequiredForLevel(levelWithProgress: Double): Long {
        val level = levelWithProgress.toInt()
        var slope = 600000L
        var xpForCurr = 7000000 + slope
        var totalXpRequired = 0L

        for (i in 61 .. level) {
            totalXpRequired += xpForCurr
            xpForCurr += slope
            if (i % 10 == 0) slope *= 2
        }

        val fractionalProgress = levelWithProgress - level
        totalXpRequired += (xpForCurr * fractionalProgress).toLong()

        val xp = if (level <= 60) {
            levelingMap.filter { it.key < level }.values.sum().toLong()
        } else {
            totalXpRequired + levelingMap.values.sum()
        }

        return xp
    }

    fun getLevelExact(neededXp: Long): Int {
        val defaultLevel = if (activeSkill in excludedSkills) 50 else 60
        return exactLevelingMap.getOrDefault(neededXp.toInt(), defaultLevel)
    }

    fun getLevel(currentXp: Long): Int {
        var level = 0
        var remainingXp = currentXp
        for ((i, v) in levelingMap) {
            if (remainingXp >= v) {
                remainingXp -= v
                level++
            }
        }
        return level
    }

    fun calculateLevelXp(levelingArray: JsonArray, level: Int): Double {
        var totalXp = 0.0
        for (i in 0 until level + 1) {
            val xp = levelingArray[i].asDouble
            totalXp += xp
        }
        return totalXp
    }

    fun levelArray(): JsonArray =
        Utils.getElement(Constants.LEVELING, "leveling_xp").asJsonArray

}
