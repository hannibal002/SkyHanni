package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.events.SkillDisplayUpdateEvent
import at.hannibal2.skyhanni.events.SkillOverflowLevelupEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
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
import java.util.LinkedList
import java.util.regex.Matcher

object SkillAPI {
    private val patternGroup = RepoPattern.group("display.skilldisplay")
    private val SKILL_PATTERN_PERCENT by patternGroup.pattern("skillpaternpercent", "\\+([\\d.,]+) (.+) \\(([\\d.]+)%\\)")
    private val SKILL_PATTERN by patternGroup.pattern("skillpattern", "\\+([\\d.,]+) (.+) \\(([\\d.,]+)/([\\d.,]+)\\)")
    private val SKILL_PATTERN_MULTIPLIER by patternGroup.pattern("skillpatternmultiplier", "\\+([\\d.,]+) (.+) \\(([\\d.,]+)/([\\d,.]+[kmb])\\)")
    private val skillTabPattern by patternGroup.pattern("skilltabpattern", "^§e§lSkills: §r§a(?<type>\\w+) (?<level>\\d+): §r§3(?<progress>.+)%\$")
    private val maxSkillTabPattern by patternGroup.pattern("maxskilltabpattern", "^§e§lSkills: §r§a(?<type>\\w+) (?<level>\\d+): §r§c§lMAX\$")
    private val SPACE_SPLITTER = Splitter.on("  ").omitEmptyStrings().trimResults()
    private var lastActionBar: String? = null
    private var levelingMap = mapOf<Int, Int>()

    //TODO: use repo ?
    private val excludedSkills = listOf("foraging", "fishing", "alchemy", "carpentry")
    var skillXPInfoMap = mutableMapOf<String, SkillXPInfo>()
    var oldSkillInfoMap = mutableMapOf<String?, SkillInfo?>()
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
    val skillMap: MutableMap<String, SkillInfo>? get() = ProfileStorageData.profileSpecific?.skillMap
    var activeSkill = ""
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
                val skillS = matcher.group(2).lowercase()
                val skillInfo = skillMap?.get(skillS) ?: SkillInfo()
                activeSkill = skillS
                when (matcher.pattern()) {
                    SKILL_PATTERN -> handleSkillPattern(matcher, skillS, skillInfo)
                    SKILL_PATTERN_PERCENT -> handleSkillPatternPercent(matcher, skillS, skillInfo)
                    SKILL_PATTERN_MULTIPLIER -> handleSkillPatternMultiplier(matcher, skillS, skillInfo)
                }
                showDisplay = true
                lastUpdate = SimpleTimeMark.now()
                SkillDisplayUpdateEvent().postAndCatch()
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

    private fun handleSkillPattern(matcher: Matcher, skillS: String, skillInfo: SkillInfo) {
        val currentXp = matcher.group(3).formatNumber()
        val maxXp = matcher.group(4).formatNumber()
        val level = getLevel(maxXp)
        val (levelOverflow, currentOverflow, currentMaxOverflow, totalOverflow) = getSkillInfo(level, currentXp, maxXp, maxXp)

        if (skillInfo.level != 0 && levelOverflow == skillInfo.level + 1)
            SkillOverflowLevelupEvent(skillS, skillInfo.level, levelOverflow).postAndCatch()

        skillInfo.apply {
            this.currentXp = currentOverflow
            currentXpMax = currentMaxOverflow
            this.level = levelOverflow
            totalXp = totalOverflow
            lastGain = matcher.group(1)
        }
        skillMap?.set(skillS, skillInfo)
    }

    private fun handleSkillPatternPercent(matcher: Matcher, skillS: String, skillInfo: SkillInfo?) {
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
        val nextLevelDiff = levelingArray[tablistLevel]?.asDouble ?: 7_600_000.0
        val nextLevelProgress = nextLevelDiff * xpPercentage / 100
        val totalXp = levelXp + nextLevelProgress
        val (_, currentOverflow, currentMaxOverflow, totalOverflow) = getSkillInfo(tablistLevel, nextLevelProgress.toLong(), nextLevelDiff.toLong(), totalXp.toLong())
        existingLevel.apply {
            this.totalXp = totalOverflow
            currentXp = currentOverflow
            currentXpMax = currentMaxOverflow
            level = tablistLevel
            lastGain = matcher.group(1)
        }
        skillMap?.set(skillS, existingLevel)
    }

    private fun handleSkillPatternMultiplier(matcher: Matcher, skillS: String, skillInfo: SkillInfo) {
        val currentXp = matcher.group(3).formatNumber()
        val maxXp = matcher.group(4).formatNumber()
        val level = getLevel(maxXp)
        val levelingArray = levelArray(skillS)
        val levelXp = calculateLevelXp(levelingArray, level - 1).toLong() + currentXp
        val (currentLevel, currentOverflow, currentMaxOverflow, totalOverflow) = getSkillInfo(level, currentXp, maxXp, levelXp)
        skillInfo.apply {
            this.currentXp = currentOverflow
            currentXpMax = currentMaxOverflow
            totalXp = totalOverflow
            this.level = currentLevel
            lastGain = matcher.group(1)
        }
        skillMap?.set(skillS, skillInfo)
    }

    private fun getSkillInfo(skillName: String): SkillInfo? {
        return skillMap?.get(skillName)
    }

    private fun getSkillInfo(currentLevel: Int, currentXp: Long, neededXp: Long, totalXp: Long): LorenzUtils.Quad<Int, Long, Long, Long> {
        return if (currentLevel == 50 && activeSkill in excludedSkills)
            calculateOverFlow50(currentXp)
        else if (currentLevel >= 60 && SkyHanniMod.feature.misc.skillProgressDisplayConfig.showOverflow)
            calculateOverFlow(currentXp)
        else
            LorenzUtils.Quad(currentLevel, currentXp, neededXp, totalXp)
    }

    /**
     * @author Soopyboo32
     */
    private fun calculateOverFlow(currentXp: Long): LorenzUtils.Quad<Int, Long, Long, Long> {
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

    private fun calculateOverFlow50(currentXp: Long): LorenzUtils.Quad<Int, Long, Long, Long> {
        var xpCurrent = currentXp
        var slope = 300000L
        var xpForCurr = 4000000 + slope
        var level = 50
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

    private fun xpRequiredForLevel(levelWithProgress: Double): Long {
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

    private fun getLevel(neededXp: Long): Int {
        val defaultLevel = if (activeSkill in excludedSkills) 50 else 60
        return levelingMap.getOrDefault(neededXp.toInt(), defaultLevel)
    }

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

    fun onCommand(it: Array<String>) {
        if (it.size != 2) {
            commandHelp()
            return
        }

        val second = it[1]
        when (it.first()) {
            "levelwithxp" -> {
                val xp = second.toLong()
                if (xp <= 111672425L) {
                    val level = getLevel(xp)
                    LorenzUtils.chat("With §b${xp.addSeparators()} §eXP you would be level §b$level")
                } else {
                    val (overflowLevel, current, needed, _) = calculateOverFlow(second.toLong())
                    LorenzUtils.chat("With §b${xp.addSeparators()} §eXP you would be level §b$overflowLevel " +
                        "§ewith progress (§b${current.addSeparators()}§e/§b${needed.addSeparators()}§e) XP")
                }
            }

            "xpforlevel" -> {
                val neededXP = xpRequiredForLevel(second.toDouble())
                LorenzUtils.chat("You need §b${neededXP.addSeparators()} §eXP to be level §b${second.toDouble()}")
            }

            else -> {
                commandHelp()
            }
        }
    }

    private fun commandHelp() {
        LorenzUtils.chat("", false)
        LorenzUtils.chat("/shskills levelwithxp <currentXP> - Get a level with the given current XP.")
        LorenzUtils.chat("/shskills xpforlevel <desiredLevel> - Get how much XP you need for a desired level.")
        LorenzUtils.chat("", false)
    }

    data class SkillInfo(var level: Int = 0, var totalXp: Long = 0, var currentXp: Long = 0, var currentXpMax: Long = 0, var lastGain: String = "")
    data class SkillXPInfo(
        var lastTotalXp: Float = 0f,
        var xpGainQueue: LinkedList<Float> = LinkedList(),
        var xpGainHour: Float = 0f,
        var xpGainLast: Float = 0f,
        var timer: Int = 3,
        var isActive: Boolean = false,
        var lastUpdate: SimpleTimeMark = SimpleTimeMark.farPast(),
    )
}
