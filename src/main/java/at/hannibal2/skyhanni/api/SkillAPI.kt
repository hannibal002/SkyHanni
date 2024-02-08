package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SkillOverflowLevelupEvent
import at.hannibal2.skyhanni.features.misc.skillprogress.SkillProgress
import at.hannibal2.skyhanni.features.misc.skillprogress.SkillType
import at.hannibal2.skyhanni.features.misc.skillprogress.SkillUtil.activeSkill
import at.hannibal2.skyhanni.features.misc.skillprogress.SkillUtil.calculateLevelXp
import at.hannibal2.skyhanni.features.misc.skillprogress.SkillUtil.calculateOverFlow
import at.hannibal2.skyhanni.features.misc.skillprogress.SkillUtil.getLevel
import at.hannibal2.skyhanni.features.misc.skillprogress.SkillUtil.getSkillInfo
import at.hannibal2.skyhanni.features.misc.skillprogress.SkillUtil.levelArray
import at.hannibal2.skyhanni.features.misc.skillprogress.SkillUtil.levelingMap
import at.hannibal2.skyhanni.features.misc.skillprogress.SkillUtil.xpRequiredForLevel
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.common.base.Splitter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.github.moulberry.notenoughupdates.util.Constants
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.LinkedList
import java.util.regex.Matcher

object SkillAPI {
    private val patternGroup = RepoPattern.group("display.skilldisplay")
    private val SKILL_PATTERN_PERCENT by patternGroup.pattern("skillpaternpercent", "\\+([\\d.,]+) (.+) \\(([\\d.]+)%\\)")
    private val SKILL_PATTERN by patternGroup.pattern("skillpattern", "\\+([\\d.,]+) (\\w+) \\(([\\d.,]+)/([\\d.,]+)\\)")
    private val SKILL_PATTERN_MULTIPLIER by patternGroup.pattern("skillpatternmultiplier", "\\+([\\d.,]+) (.+) \\(([\\d.,]+)/([\\d,.]+[kmb])\\)")
    private val skillTabPattern by patternGroup.pattern("skilltabpattern", "^§e§lSkills: §r§a(?<type>\\w+) (?<level>\\d+): §r§3(?<progress>.+)%\$")
    private val maxSkillTabPattern by patternGroup.pattern("maxskilltabpattern", "^§e§lSkills: §r§a(?<type>\\w+) (?<level>\\d+): §r§c§lMAX\$")
    private val SPACE_SPLITTER = Splitter.on("  ").omitEmptyStrings().trimResults()

    var skillXPInfoMap = mutableMapOf<SkillType, SkillXPInfo>()
    var oldSkillInfoMap = mutableMapOf<SkillType?, SkillInfo?>()
    val skillMap: MutableMap<SkillType, SkillInfo>? get() = ProfileStorageData.profileSpecific?.skillMap
    var showDisplay = false
    var lastUpdate = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onActionBar(event: ActionBarUpdateEvent) {
        val actionBar = event.actionBar.removeColor()
        val components = SPACE_SPLITTER.splitToList(actionBar)

        for (component in components) {
            val matcher = listOf(SKILL_PATTERN, SKILL_PATTERN_PERCENT, SKILL_PATTERN_MULTIPLIER)
                .firstOrNull { it.matcher(component).matches() }
                ?.matcher(component)

            if (matcher?.matches() == true) {
                val skillName = matcher.group(2)
                val skill = SkillType.getByNameFirstUppercase(skillName) ?: return
                val skillInfo = skillMap?.get(skill) ?: SkillInfo()
                activeSkill = skill
                when (matcher.pattern()) {
                    SKILL_PATTERN -> handleSkillPattern(matcher, skill, skillInfo)
                    SKILL_PATTERN_PERCENT -> handleSkillPatternPercent(matcher, skill, skillInfo)
                    SKILL_PATTERN_MULTIPLIER -> handleSkillPatternMultiplier(matcher, skill, skillInfo)
                }
                showDisplay = true
                lastUpdate = SimpleTimeMark.now()
                SkillProgress.updateDisplay()
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

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        val inventoryName = event.inventoryName
        for (stack in event.inventoryItems.values) {
            val lore = stack.getLore()
            if (inventoryName == "Your Skills" &&
                lore.any { it.contains("Click to view!") }
            ) {
                val cleanName = stack.cleanName()
                val split = cleanName.split(" ")
                val skillName = split.first()
                val skill = SkillType.getByNameFirstUppercase(skillName) ?: continue
                val skillLevel = if (split.size > 1) split.last().romanToDecimalIfNecessary() else 0
                val skillInfo = skillMap?.getOrPut(skill) { SkillInfo() }

                for ((lineIndex, line) in lore.withIndex()) {
                    val cleanLine = line.removeColor()
                    if (!cleanLine.startsWith("                    ")) continue
                    val previousLine = stack.getLore()[lineIndex - 1]
                    val progress = cleanLine.substring(cleanLine.lastIndexOf(' ') + 1)
                    if (previousLine == "§7§8Max Skill level reached!") {
                        var totalXp = progress.formatNumber()
                        val minus = if (skillLevel == 50) 4_000_000 else if (skillLevel == 60) 7_000_000 else 0
                        totalXp -= minus
                        val (overflowLevel, overflowCurrent, overflowNeeded, overflowTotal) = getSkillInfo(skillLevel, totalXp, 0L, totalXp)
                        skillInfo?.apply {
                            this.overflowLevel = overflowLevel
                            this.overflowCurrentXp = overflowCurrent
                            this.overflowCurrentXpMax = overflowNeeded
                            this.overflowTotalXp = overflowTotal

                            this.totalXp = totalXp
                            this.level = skillLevel
                            this.currentXp = totalXp
                            this.currentXpMax = 0L
                        }
                    } else {
                        val splitProgress = progress.split("/")
                        val currentXp = splitProgress.first().formatNumber()
                        val neededXp = splitProgress.last().formatNumber()
                        val levelingArray = levelArray()
                        val levelXp = calculateLevelXp(levelingArray, skillLevel - 1).toLong()

                        skillInfo?.apply {
                            this.currentXp = currentXp
                            this.level = skillLevel
                            this.currentXpMax = neededXp
                            this.totalXp = levelXp + currentXp

                            this.overflowCurrentXp = currentXp
                            this.overflowLevel = skillLevel
                            this.overflowCurrentXpMax = neededXp
                            this.overflowTotalXp = levelXp + currentXp
                        }
                    }
                }
            }
        }
    }

    private fun handleSkillPattern(matcher: Matcher, skill: SkillType, skillInfo: SkillInfo) {
        val currentXp = matcher.group(3).formatNumber()
        val maxXp = matcher.group(4).formatNumber()
        val level = getLevel(maxXp)

        val (levelOverflow, currentOverflow, currentMaxOverflow, totalOverflow) = getSkillInfo(level, currentXp, maxXp, currentXp)
        if (skillInfo.overflowLevel != 0 && levelOverflow == skillInfo.overflowLevel + 1)
            SkillOverflowLevelupEvent(skill, skillInfo.overflowLevel, levelOverflow).postAndCatch()

        skillInfo.apply {
            this.level = level
            this.currentXp = currentXp
            this.currentXpMax = maxXp
            this.totalXp = currentXp

            this.overflowLevel = levelOverflow
            this.overflowCurrentXp = currentOverflow
            this.overflowCurrentXpMax = currentMaxOverflow
            this.overflowTotalXp = totalOverflow

            this.lastGain = matcher.group(1)
        }
        skillMap?.set(skill, skillInfo)
    }

    private fun handleSkillPatternPercent(matcher: Matcher, skill: SkillType, skillInfo: SkillInfo?) {
        var tablistLevel = 0
        for (line in TabListData.getTabList()) {
            var levelMatcher = skillTabPattern.matcher(line)
            if (levelMatcher.matches()) {
                tablistLevel = levelMatcher.group("level").toInt()
                if (levelMatcher.group("type").lowercase() != activeSkill?.lowercaseName) tablistLevel = 0
            } else {
                levelMatcher = maxSkillTabPattern.matcher(line)
                if (levelMatcher.matches()) {
                    tablistLevel = levelMatcher.group("level").toInt()
                    if (levelMatcher.group("type").lowercase() != activeSkill?.lowercaseName) tablistLevel = 0
                }
            }
        }
        val existingLevel = getSkillInfo(skill) ?: SkillInfo()
        val xpPercentageS = matcher.group(3).replace(",", "")
        val xpPercentage = xpPercentageS.toFloatOrNull() ?: return
        val levelingArray = levelArray()
        val levelXp = calculateLevelXp(levelingArray, existingLevel.level - 1)
        val nextLevelDiff = levelingArray[tablistLevel]?.asDouble ?: 7_600_000.0
        val nextLevelProgress = nextLevelDiff * xpPercentage / 100
        val totalXp = levelXp + nextLevelProgress
        val (_, currentOverflow, currentMaxOverflow, totalOverflow) = getSkillInfo(tablistLevel, nextLevelProgress.toLong(), nextLevelDiff.toLong(), totalXp.toLong())
        existingLevel.apply {
            this.totalXp = totalXp.toLong()
            this.currentXp = nextLevelProgress.toLong()
            this.currentXpMax = nextLevelDiff.toLong()
            this.level = tablistLevel

            this.overflowTotalXp = totalOverflow
            this.overflowCurrentXp = currentOverflow
            this.overflowCurrentXpMax = currentMaxOverflow
            this.overflowLevel = tablistLevel

            this.lastGain = matcher.group(1)
        }
        skillMap?.set(skill, existingLevel)
    }

    private fun handleSkillPatternMultiplier(matcher: Matcher, skillS: SkillType, skillInfo: SkillInfo) {
        val currentXp = matcher.group(3).formatNumber()
        val maxXp = matcher.group(4).formatNumber()
        val level = getLevel(maxXp)
        val levelingArray = levelArray()
        val levelXp = calculateLevelXp(levelingArray, level - 1).toLong() + currentXp
        val (currentLevel, currentOverflow, currentMaxOverflow, totalOverflow) = getSkillInfo(level, currentXp, maxXp, levelXp)
        skillInfo.apply {
            this.overflowCurrentXp = currentOverflow
            this.overflowCurrentXpMax = currentMaxOverflow
            this.overflowTotalXp = totalOverflow
            this.overflowLevel = currentLevel

            this.currentXp = currentXp
            this.currentXpMax = maxXp
            this.totalXp = levelXp
            this.level = level

            this.lastGain = matcher.group(1)
        }
        skillMap?.set(skillS, skillInfo)
    }

    fun onCommand(it: Array<String>) {
        if (it.isEmpty()) {
            commandHelp()
            return
        }

        if (it.size == 2) {
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
            }
        }

        if (it.size == 1) {
            when (it.first()) {
                "copytoclipboard" -> {
                    skillMap?.let {
                        val list = buildList {
                            add("```md")
                            for ((skillName, skillInfo) in it) {
                                add("Skill: $skillName")
                                add("-  Level: ${skillInfo.level}")
                                add("-  CurrentXp: ${skillInfo.currentXp}")
                                add("-  CurrentXpMax: ${skillInfo.currentXpMax}")
                                add("-  TotalXp: ${skillInfo.totalXp}")
                                add("-  OverflowLevel: ${skillInfo.overflowLevel}")
                                add("-  OverflowCurrentXp: ${skillInfo.overflowCurrentXp}")
                                add("-  OverflowCurrentXpMax: ${skillInfo.overflowCurrentXpMax}")
                                add("-  OverflowTotalXp: ${skillInfo.overflowTotalXp}")
                                add(" ")
                            }
                            add("```")
                        }
                        OSUtils.copyToClipboard(list.joinToString("\n"))
                        LorenzUtils.chat("Copied to clipboard!")
                        return
                    }
                }

                else -> {
                    commandHelp()
                }
            }
        }
    }

    private fun commandHelp() {
        LorenzUtils.chat("", false)
        LorenzUtils.chat("/shskills levelwithxp <currentXP> - Get a level with the given current XP.")
        LorenzUtils.chat("/shskills xpforlevel <desiredLevel> - Get how much XP you need for a desired level.")
        LorenzUtils.chat("/shskills copytoclipboard - Copy your skills information into your clipboard.")
        LorenzUtils.chat("", false)
    }

    data class SkillInfo(
        var level: Int = 0,
        var totalXp: Long = 0,
        var currentXp: Long = 0,
        var currentXpMax: Long = 0,
        var overflowLevel: Int = 0,
        var overflowCurrentXp: Long = 0,
        var overflowTotalXp: Long = 0,
        var overflowCurrentXpMax: Long = 0,
        var lastGain: String = "")

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
