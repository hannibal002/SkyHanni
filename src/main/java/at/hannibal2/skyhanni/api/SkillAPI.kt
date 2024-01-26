package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.common.base.Splitter
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import io.github.moulberry.notenoughupdates.util.Constants
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.io.Serializable
import java.util.regex.Matcher

object SkillAPI {
    private val patternGroup = RepoPattern.group("display.skilldisplay")
    private val SKILL_PATTERN_PERCENT by patternGroup.pattern("skillpaternpercent", "\\+(\\d+(?:,\\d+)*(?:\\.\\d+)?) (.+) \\((\\d\\d?(?:\\.\\d\\d?)?)%\\)")
    private val SKILL_PATTERN by patternGroup.pattern("skillpattern", "\\+(\\d+(?:,\\d+)*(?:\\.\\d+)?) (.+) \\((\\d+(?:,\\d+)*(?:\\.\\d+)?)/(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\)")
    private val SKILL_PATTERN_MULTIPLIER by patternGroup.pattern("skillpatternmultiplier", "\\+(\\d+(?:,\\d+)*(?:\\.\\d+)?) (.+) \\((\\d+(?:,\\d+)*(?:\\.\\d+)?)/(\\d+(?:.\\d+)*(?:k|m|b))\\)")
    private val skillTabPattern by patternGroup.pattern("skilltabpattern", "^§e§lSkills: §r§a(?<type>\\w+) (?<level>\\d+): §r§3(?<progress>.+)%\$")
    private val maxSkillTabPattern by patternGroup.pattern("maxskilltabpattern", "^§e§lSkills: §r§a(?<type>\\w+) (?<level>\\d+): §r§c§lMAX\$")
    private val SPACE_SPLITTER = Splitter.on("  ").omitEmptyStrings().trimResults()
    private var lastActionBar: String? = null
    private var levelingMap = mapOf<Int, Int>()
    val skillMap: MutableMap<String, SkillInfo>? get() = ProfileStorageData.profileSpecific?.skillMap
    var activeSkill = ""
    var gained = ""
    var showDisplay = false
    var lastUpdate = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        val actionBar = event.message.removeColor()

        if (lastActionBar != null && lastActionBar == actionBar) return
        lastActionBar = actionBar
        val components = SPACE_SPLITTER.splitToList(actionBar)

        for (component in components) {
            val matcher = listOf(SKILL_PATTERN, SKILL_PATTERN_PERCENT, SKILL_PATTERN_MULTIPLIER)
                .firstOrNull { it.matcher(component).matches() }
                ?.matcher(component)

            if (matcher?.matches() == true) {
                gained = matcher.group(1)
                val skillS = matcher.group(2).lowercase()
                when (matcher.pattern()) {
                    SKILL_PATTERN -> handleSkillPattern(matcher, skillS)
                    SKILL_PATTERN_PERCENT -> handleSkillPatternPercent(matcher, skillS)
                    SKILL_PATTERN_MULTIPLIER -> handleSkillPatternMultiplier(matcher, skillS)
                }
                lastUpdate = SimpleTimeMark.now()
                showDisplay = true
                return
            }
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val gson = GsonBuilder().create()
        val xpList: List<Int> = gson.fromJson(
            Utils.getElement(Constants.LEVELING, "leveling_xp").asJsonArray.toString(),
            object : TypeToken<List<Int>>() {}.type
        )
        levelingMap = xpList.withIndex().associate { (index, xp) -> xp to index }
    }

    private fun handleSkillPattern(matcher: Matcher, skillS: String) {
        val currentXp = matcher.group(3).formatNumber()
        val maxXp = matcher.group(4).formatNumber()
        val skillInfo = SkillInfo()
        val level = getLevel(maxXp)
        val (levelOverflow, currentOverflow, currentMaxOverflow, totalOverflow) = getSkillInfo(level, currentXp, maxXp, maxXp)

        skillInfo.apply {
            this.currentXp = currentOverflow
            currentXpMax = currentMaxOverflow
            this.level = levelOverflow
            totalXp = totalOverflow
        }
        skillMap?.set(skillS, skillInfo)
        activeSkill = skillS
    }

    private fun handleSkillPatternPercent(matcher: Matcher, skillS: String) {
        var tablistLevel = 0
        for (line in TabListData.getTabList()) {
            var levelMatcher = skillTabPattern.matcher(line)
            if (levelMatcher.matches()) {
                tablistLevel = levelMatcher.group("level").toInt()
                if (levelMatcher.group("type").lowercase() != activeSkill.lowercase()) tablistLevel = 0
            } else {
                levelMatcher = maxSkillTabPattern.matcher(line)
                if (levelMatcher.matches()) {
                    tablistLevel = levelMatcher.group("level").toInt()
                    if (levelMatcher.group("type").lowercase() != activeSkill.lowercase()) tablistLevel = 0
                }
            }
        }
        val existingLevel = getSkillInfo(skillS) ?: SkillInfo()
        val xpPercentageS = matcher.group(3).replace(",", "")
        val xpPercentage = xpPercentageS.toFloatOrNull() ?: return
        val levelingArray = levelArray(skillS)
        val levelXp = calculateLevelXp(levelingArray, existingLevel.level - 1)
        val nextLevelDiff = levelingArray[tablistLevel].asDouble
        val nextLevelProgress = nextLevelDiff * xpPercentage / 100
        val totalXp = levelXp + nextLevelProgress
        val (_, currentOverflow, currentMaxOverflow, totalOverflow) = getSkillInfo(tablistLevel, nextLevelProgress.toLong(), nextLevelDiff.toLong(), totalXp.toLong())
        existingLevel.apply {
            this.totalXp = totalOverflow
            currentXp = currentOverflow
            currentXpMax = currentMaxOverflow
            level = tablistLevel
        }
        skillMap?.set(skillS, existingLevel)
        activeSkill = skillS
    }

    private fun handleSkillPatternMultiplier(matcher: Matcher, skillS: String) {
        val currentXp = matcher.group(3).formatNumber()
        val maxXp = matcher.group(4).formatNumber()
        val skillInfo = SkillInfo()
        val level = getLevel(maxXp)
        val levelingArray = levelArray(skillS)
        val levelXp = calculateLevelXp(levelingArray, level - 1).toLong() + currentXp
        val (currentLevel, currentOverflow, currentMaxOverflow, totalOverflow) = getSkillInfo(level, currentXp, maxXp, levelXp)
        skillInfo.apply {
            this.currentXp = currentOverflow
            currentXpMax = currentMaxOverflow
            totalXp = totalOverflow
            this.level = currentLevel
        }
        skillMap?.set(skillS, skillInfo)
        activeSkill = skillS
    }

    private fun getSkillInfo(skillName: String): SkillInfo? {
        return skillMap?.get(skillName)
    }

    private fun getSkillInfo(currentLevel: Int, currentXp: Long, neededXp: Long, totalXp: Long): Quad<Int, Long, Long, Long> {
        val (level, overflowExp, xpForCurrentLevel, total) = calculateOverFlow(currentXp)
        return if (currentLevel >= 60 && SkyHanniMod.feature.misc.skillProgressDisplayConfig.showOverflow.get()) Quad(level, overflowExp, xpForCurrentLevel, total) else Quad(currentLevel, currentXp, neededXp, totalXp)
    }

    /**
     * @author Soopyboo32
     */
    private fun calculateOverFlow(currentXp: Long): Quad<Int, Long, Long, Long> {
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

    fun calculateXpForLevel(targetLevel: Double): Long {
        if (targetLevel < 60.0) return 0L
        val integralPart = targetLevel.toInt()
        val fractionalPart = targetLevel - integralPart
        var level = 60
        var totalXpNeeded = 0L
        var slope = 600000L
        var xpForNextLevel = 7000000L + slope

        while (level < integralPart) {
            totalXpNeeded += xpForNextLevel
            xpForNextLevel += slope
            level++
            if (level % 10 == 0) slope *= 2
        }

        if (fractionalPart > 0.0) {
            totalXpNeeded += (xpForNextLevel * fractionalPart).toLong()
        }

        return totalXpNeeded
    }

    private fun getLevel(neededXp: Long): Int {
        return levelingMap.getOrDefault(neededXp.toInt(), 60)
    }

    val stackMap = mapOf(
        "Farming" to Utils.createItemStack(Items.golden_hoe, "Farming"),
        "Combat" to Utils.createItemStack(Items.golden_sword, "Combat"),
        "Foraging" to Utils.createItemStack(Items.golden_axe, "Foraging"),
        "Alchemy" to Utils.createItemStack(Items.brewing_stand, "Alchemy"),
        "Mining" to Utils.createItemStack(Items.golden_pickaxe, "Mining"),
        "Enchanting" to Utils.createItemStack(Blocks.enchanting_table, "Enchanting"),
        "Fishing" to Utils.createItemStack(Items.fishing_rod, "Fishing"),
        "Carpentry" to Utils.createItemStack(Blocks.crafting_table, "Carpentry")
    )

    private fun calculateLevelXp(levelingArray: JsonArray, level: Int): Double {
        var totalXp = 0.0
        for (i in 0 until level + 1) {
            val xp = levelingArray[i].asDouble
            totalXp += xp
        }
        return totalXp
    }

    private fun levelArray(skillType: String): JsonArray =
        if (skillType == "Runecrafting") Utils.getElement(Constants.LEVELING, "runecrafting_xp").asJsonArray
        else Utils.getElement(Constants.LEVELING, "leveling_xp").asJsonArray

    data class SkillInfo(var level: Int = 0, var totalXp: Long = 0, var currentXp: Long = 0, var currentXpMax: Long = 0)

    data class Quad<out A, out B, out C, out D>(
        val first: A,
        val second: B,
        val third: C,
        val quad: D
    ) : Serializable {
        override fun toString(): String = "($first, $second, $third, $quad)"
    }
}
