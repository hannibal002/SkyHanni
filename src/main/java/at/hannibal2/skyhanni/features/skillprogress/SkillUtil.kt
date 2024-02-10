package at.hannibal2.skyhanni.features.skillprogress

import at.hannibal2.skyhanni.api.SkillAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import com.google.gson.JsonArray
import io.github.moulberry.notenoughupdates.util.Constants
import io.github.moulberry.notenoughupdates.util.Utils

object SkillUtil {

    var levelingMap = mapOf<Int, Int>()
    var activeSkill: SkillType = SkillType.NONE
    private val excludedSkills = listOf(
        SkillType.FORAGING,
        SkillType.FISHING,
        SkillType.ALCHEMY,
        SkillType.CARPENTRY
    )

    fun getSkillInfo(skill: SkillType): SkillAPI.SkillInfo? {
        return SkillAPI.skillMap?.get(skill)
    }

    fun getSkillInfo(currentLevel: Int, currentXp: Long, neededXp: Long, totalXp: Long): LorenzUtils.Quad<Int, Long, Long, Long> {
        return if (currentLevel == 50 && neededXp == 0L)
            calculateOverFlow50(currentXp)
        else if (currentLevel >= 60)
            calculateOverFlow(currentXp)
        else
            LorenzUtils.Quad(currentLevel, currentXp, neededXp, totalXp)
    }

    /**
     * @author Soopyboo32
     */
    fun calculateOverFlow(currentXp: Long): LorenzUtils.Quad<Int, Long, Long, Long> {
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
        return LorenzUtils.Quad(level, xpCurrent, xpForCurr, total)
    }

    /**
     * Calculate overflow starting at level 50
     */
    private fun calculateOverFlow50(currentXp: Long): LorenzUtils.Quad<Int, Long, Long, Long> {
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

        return LorenzUtils.Quad(level, xpCurrent, xpForCurr, total)
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

        return totalXpRequired
    }

    fun getLevel(neededXp: Long): Int {
        val defaultLevel = if (activeSkill in excludedSkills) 50 else 60
        return levelingMap.getOrDefault(neededXp.toInt(), defaultLevel)
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
