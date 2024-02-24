package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SkillOverflowLevelupEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillProgress
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.SPACE_SPLITTER
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.XP_NEEDED_FOR_60
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.calculateLevelXp
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.calculateOverFlow
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.getLevel
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.getLevelExact
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.getSkillInfo
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.levelArray
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.xpRequiredForLevel
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ChatUtils.userError
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import io.github.moulberry.notenoughupdates.util.Constants
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.command.CommandBase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.LinkedList
import java.util.regex.Matcher
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.seconds

object SkillAPI {
    private val patternGroup = RepoPattern.group("api.skilldisplay")
    private val skillPercentPattern by patternGroup.pattern(
        "skill.percent",
        "\\+(?<gained>[\\d.,]+) (?<skillName>.+) \\((?<progress>[\\d.]+)%\\)"
    )
    private val skillPattern by patternGroup.pattern(
        "skill",
        "\\+(?<gained>[\\d.,]+) (?<skillName>\\w+) \\((?<current>[\\d.,]+)/(?<needed>[\\d.,]+)\\)"
    )
    private val skillMultiplierPattern by patternGroup.pattern(
        "skill.multiplier",
        "\\+(?<gained>[\\d.,]+) (?<skillName>.+) \\((?<current>[\\d.,]+)/(?<needed>[\\d,.]+[kmb])\\)"
    )
    private val skillTabPattern by patternGroup.pattern(
        "skill.tab",
        "^§e§lSkills: §r§a(?<type>\\w+) (?<level>\\d+): §r§3(?<progress>.+)%\$"
    )
    private val maxSkillTabPattern by patternGroup.pattern(
        "skill.tab.max",
        "^§e§lSkills: §r§a(?<type>\\w+) (?<level>\\d+): §r§c§lMAX\$"
    )

    var skillXPInfoMap = mutableMapOf<SkillType, SkillXPInfo>()
    var oldSkillInfoMap = mutableMapOf<SkillType?, SkillInfo?>()
    val storage get() = ProfileStorageData.profileSpecific?.skillData
    var exactLevelingMap = mapOf<Int, Int>()
    var levelingMap = mapOf<Int, Int>()
    var activeSkill: SkillType? = null

    // TODO Use a map maxSkillLevel and move it into the repo
    val excludedSkills = listOf(
        SkillType.FORAGING,
        SkillType.FISHING,
        SkillType.ALCHEMY,
        SkillType.CARPENTRY
    )
    var showDisplay = false
    var lastUpdate = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onActionBar(event: ActionBarUpdateEvent) {
        val actionBar = event.actionBar.removeColor()
        val components = SPACE_SPLITTER.splitToList(actionBar)
        for (component in components) {
            val matcher = listOf(skillPattern, skillPercentPattern, skillMultiplierPattern)
                .firstOrNull { it.matcher(component).matches() }
                ?.matcher(component)

            if (matcher?.matches() == true) {
                val skillName = matcher.group("skillName")
                val skillType = SkillType.getByNameOrNull(skillName) ?: return
                val skillInfo = storage?.get(skillType) ?: SkillInfo()
                val skillXp = skillXPInfoMap[skillType] ?: SkillXPInfo()
                activeSkill = skillType
                when (matcher.pattern()) {
                    skillPattern -> handleSkillPattern(matcher, skillType, skillInfo)
                    skillPercentPattern -> handleSkillPatternPercent(matcher, skillType)
                    skillMultiplierPattern -> handleSkillPatternMultiplier(matcher, skillType, skillInfo)
                }
                showDisplay = true
                lastUpdate = SimpleTimeMark.now()
                skillXp.lastUpdate = SimpleTimeMark.now()
                skillXp.sessionTimerActive = true

                if (skillXp.shouldStartTimer) {
                    runTimer(skillName, skillXp)
                    skillXp.shouldStartTimer = false
                }
                SkillProgress.updateDisplay()
                SkillProgress.hideInActionBar = listOf(component)
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
        levelingMap = xpList.withIndex().associate { (index, xp) -> index to xp }
        exactLevelingMap = xpList.withIndex().associate { (index, xp) -> xp to index }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        val inventoryName = event.inventoryName
        for (stack in event.inventoryItems.values) {
            val lore = stack.getLore()
            if (inventoryName == "Your Skills" &&
                lore.any { it.contains("Click to view!") || it.contains("Not unlocked!") }
            ) {
                val cleanName = stack.cleanName()
                val split = cleanName.split(" ")
                val skillName = split.first()
                val skill = SkillType.getByNameOrNull(skillName) ?: continue
                val skillLevel = if (split.size > 1) split.last().romanToDecimalIfNecessary() else 0
                val skillInfo = storage?.getOrPut(skill) { SkillInfo() }

                for ((lineIndex, line) in lore.withIndex()) {
                    val cleanLine = line.removeColor()
                    if (!cleanLine.startsWith("                    ")) continue
                    val previousLine = stack.getLore()[lineIndex - 1]
                    val progress = cleanLine.substring(cleanLine.lastIndexOf(' ') + 1)
                    if (previousLine == "§7§8Max Skill level reached!") {
                        var totalXp = progress.formatNumber()
                        val minus = if (skillLevel == 50) 4_000_000 else if (skillLevel == 60) 7_000_000 else 0
                        totalXp -= minus
                        val (overflowLevel, overflowCurrent, overflowNeeded, overflowTotal) = getSkillInfo(
                            skillLevel,
                            totalXp,
                            0L,
                            totalXp
                        )
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

    @SubscribeEvent
    fun onDebugDataCollectCurrent(event: DebugDataCollectEvent) {
        event.title("Current Skill")
        val storage = storage
        if (storage == null) {
            event.addIrrelevant("SkillMap is empty")
            return
        }

        val skillType = activeSkill
        if (skillType == null) {
            event.addIrrelevant("activeSkill is null")
            return
        }

        event.addData {
            storage[skillType]?.let { skillInfo ->
                addDebug(skillType, skillInfo)
            }
        }
    }

    @SubscribeEvent
    fun onDebugDataCollectAll(event: DebugDataCollectEvent) {
        event.title("All Skills")
        val storage = storage
        if (storage == null) {
            event.addIrrelevant("SkillMap is empty")
            return
        }

        event.addIrrelevant {
            for ((skillType, skillInfo) in storage) {
                addDebug(skillType, skillInfo)
            }
        }
    }

    private fun MutableList<String>.addDebug(skillType: SkillType, skillInfo: SkillInfo) {
        add("Name: $skillType")
        add("-  Level: ${skillInfo.level}")
        add("-  CurrentXp: ${skillInfo.currentXp}")
        add("-  CurrentXpMax: ${skillInfo.currentXpMax}")
        add("-  TotalXp: ${skillInfo.totalXp}")
        add("-  OverflowLevel: ${skillInfo.overflowLevel}")
        add("-  OverflowCurrentXp: ${skillInfo.overflowCurrentXp}")
        add("-  OverflowCurrentXpMax: ${skillInfo.overflowCurrentXpMax}")
        add("-  OverflowTotalXp: ${skillInfo.overflowTotalXp}")
        add("-  CustomGoalLevel: ${skillInfo.customGoalLevel}\n")
    }

    private fun runTimer(skillName: String, info: SkillXPInfo) {
        fixedRateTimer(name = "skyhanni-skillprogress-timer-$skillName", initialDelay = 1_000L, period = 1_000L) {
            if (info.shouldStartTimer) cancel()
            val time = when (activeSkill) {
                SkillType.FARMING -> SkillProgress.etaConfig.farmingPauseTime
                SkillType.MINING -> SkillProgress.etaConfig.miningPauseTime
                SkillType.COMBAT -> SkillProgress.etaConfig.combatPauseTime
                SkillType.FORAGING -> SkillProgress.etaConfig.foragingPauseTime
                SkillType.FISHING -> SkillProgress.etaConfig.fishingPauseTime
                else -> 0
            }
            if (info.lastUpdate.passedSince() > time.seconds) {
                info.sessionTimerActive = false
            }
            if (info.sessionTimerActive) {
                info.timeActive++
            }
        }
    }

    private fun handleSkillPattern(matcher: Matcher, skillType: SkillType, skillInfo: SkillInfo) {
        val currentXp = matcher.group("current").formatNumber()
        val maxXp = matcher.group("needed").formatNumber()
        val level = getLevelExact(maxXp)

        val (levelOverflow, currentOverflow, currentMaxOverflow, totalOverflow) = getSkillInfo(
            level,
            currentXp,
            maxXp,
            currentXp
        )
        if (skillInfo.overflowLevel != 0 && levelOverflow == skillInfo.overflowLevel + 1)
            SkillOverflowLevelupEvent(skillType, skillInfo.overflowLevel, levelOverflow).postAndCatch()

        skillInfo.apply {
            this.level = level
            this.currentXp = currentXp
            this.currentXpMax = maxXp
            this.totalXp = currentXp

            this.overflowLevel = levelOverflow
            this.overflowCurrentXp = currentOverflow
            this.overflowCurrentXpMax = currentMaxOverflow
            this.overflowTotalXp = totalOverflow

            this.lastGain = matcher.group("gained")
        }
        storage?.set(skillType, skillInfo)
    }

    private fun handleSkillPatternPercent(matcher: Matcher, skillType: SkillType) {
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
        val existingLevel = getSkillInfo(skillType) ?: SkillInfo()
        val xpPercentageS = matcher.group("progress").replace(",", "")
        val xpPercentage = xpPercentageS.toFloatOrNull() ?: return
        val levelingArray = levelArray()
        val levelXp = calculateLevelXp(levelingArray, existingLevel.level - 1)
        val nextLevelDiff = levelingArray[tablistLevel]?.asDouble ?: 7_600_000.0
        val nextLevelProgress = nextLevelDiff * xpPercentage / 100
        val totalXp = levelXp + nextLevelProgress
        val (_, currentOverflow, currentMaxOverflow, totalOverflow) = getSkillInfo(
            tablistLevel,
            nextLevelProgress.toLong(),
            nextLevelDiff.toLong(),
            totalXp.toLong()
        )
        existingLevel.apply {
            this.totalXp = totalXp.toLong()
            this.currentXp = nextLevelProgress.toLong()
            this.currentXpMax = nextLevelDiff.toLong()
            this.level = tablistLevel

            this.overflowTotalXp = totalOverflow
            this.overflowCurrentXp = currentOverflow
            this.overflowCurrentXpMax = currentMaxOverflow
            this.overflowLevel = tablistLevel

            this.lastGain = matcher.group("gained")
        }
        storage?.set(skillType, existingLevel)
    }

    private fun handleSkillPatternMultiplier(matcher: Matcher, skillType: SkillType, skillInfo: SkillInfo) {
        val currentXp = matcher.group("current").formatNumber()
        val maxXp = matcher.group("needed").formatNumber()
        val level = getLevelExact(maxXp)
        val levelingArray = levelArray()
        val levelXp = calculateLevelXp(levelingArray, level - 1).toLong() + currentXp
        val (currentLevel, currentOverflow, currentMaxOverflow, totalOverflow) = getSkillInfo(
            level,
            currentXp,
            maxXp,
            levelXp
        )
        skillInfo.apply {
            this.overflowCurrentXp = currentOverflow
            this.overflowCurrentXpMax = currentMaxOverflow
            this.overflowTotalXp = totalOverflow
            this.overflowLevel = currentLevel

            this.currentXp = currentXp
            this.currentXpMax = maxXp
            this.totalXp = levelXp
            this.level = level

            this.lastGain = matcher.group("gained")
        }
        storage?.set(skillType, skillInfo)
    }

    fun onCommand(it: Array<String>) {
        if (it.isEmpty()) {
            commandHelp()
            return
        }

        val first = it.first()
        if (it.size == 1) {
            when (first) {
                "goal" -> {
                    ChatUtils.chat("§bSkill Custom Goal Level")
                    val map = storage?.filter { it.value.customGoalLevel != 0 } ?: return
                    if (map.isEmpty()) {
                        ChatUtils.chat("§cYou haven't set any custom goals yet!")
                    }
                    map.forEach { (skill, data) ->
                        ChatUtils.chat("§e${skill.displayName}: §b${data.customGoalLevel}")
                    }
                    return
                }
            }
        }

        if (it.size == 2) {
            val second = it[1]
            when (first) {
                "levelwithxp" -> {
                    val xp = second.toLong()
                    if (xp <= XP_NEEDED_FOR_60) {
                        val level = getLevel(xp)
                        ChatUtils.chat("With §b${xp.addSeparators()} §eXP you would be level §b$level")
                    } else {
                        val (overflowLevel, current, needed, _) = calculateOverFlow(second.toLong())
                        ChatUtils.chat(
                            "With §b${xp.addSeparators()} §eXP you would be level §b$overflowLevel " +
                                "§ewith progress (§b${current.addSeparators()}§e/§b${needed.addSeparators()}§e) XP"
                        )
                    }
                    return
                }

                "xpforlevel" -> {
                    val level = second.toInt()
                    if (level <= 60) {
                        val neededXp = levelingMap.filter { it.key < level }.values.sum().toLong()
                        ChatUtils.chat("You need §b${neededXp.addSeparators()} §eXP to be level §b${level.toDouble()}")
                    } else {
                        val base = levelingMap.values.sum().toLong()
                        val neededXP = xpRequiredForLevel(level.toDouble()) + base
                        ChatUtils.chat("You need §b${neededXP.addSeparators()} §eXP to be level §b${level.toDouble()}")
                    }
                    return
                }

                "goal" -> {
                    val rawSkill = it[1].lowercase()
                    val skillType = SkillType.getByNameOrNull(rawSkill)
                    if (skillType == null) {
                        userError("Unknown Skill type: $rawSkill")
                        return
                    }
                    val skill = storage?.get(skillType) ?: return
                    skill.customGoalLevel = 0
                    ChatUtils.chat("Custom goal level for §b${skillType.displayName} §ereset")
                }
            }
        }
        if (it.size == 3) {
            when (first) {
                "goal" -> {
                    val rawSkill = it[1].lowercase()
                    val skillType = SkillType.getByNameOrNull(rawSkill)
                    if (skillType == null) {
                        userError("Unknown Skill type: $rawSkill")
                        return
                    }
                    val rawLevel = it[2]
                    val targetLevel = rawLevel.toIntOrNull()
                    if (targetLevel == null) {
                        userError("$rawLevel is not a valid number.")
                        return
                    }
                    val skill = storage?.get(skillType) ?: return

                    if (targetLevel <= skill.overflowLevel) {
                        userError("Custom goal level ($targetLevel) must be greater than your current level (${skill.overflowLevel}).")
                        return
                    }

                    skill.customGoalLevel = targetLevel
                    ChatUtils.chat("Custom goal level for §b${skillType.displayName} §eset to §b$targetLevel")
                    return
                }
            }
        }
        commandHelp()
    }

    fun onComplete(strings: Array<String>): List<String> {
        return when (strings.size) {
            1 -> listOf("levelwithxp", "xpforlevel", "goal")
            2 -> if (strings[0].lowercase() == "goal") CommandBase.getListOfStringsMatchingLastWord(
                strings,
                SkillType.entries.map { it.displayName })
            else
                listOf()

            else -> listOf()
        }
    }

    private fun commandHelp() {
        ChatUtils.chat(
            listOf(
                "§6/shskills levelwithxp <currentXP> - §bGet a level with the given current XP.",
                "§6/shskills xpforlevel <desiredLevel> - §bGet how much XP you need for a desired level.",
                "§6/shskills goal - §bView your current goal",
                "§6/shskills goal <skill> <level> - §bDefine your goal for <skill>",
                "",
            ).joinToString("\n"), prefix = false
        )
    }

    data class SkillInfo(
        @Expose var level: Int = 0,
        @Expose var totalXp: Long = 0,
        @Expose var currentXp: Long = 0,
        @Expose var currentXpMax: Long = 0,
        @Expose var overflowLevel: Int = 0,
        @Expose var overflowCurrentXp: Long = 0,
        @Expose var overflowTotalXp: Long = 0,
        @Expose var overflowCurrentXpMax: Long = 0,
        @Expose var lastGain: String = "",
        @Expose var customGoalLevel: Int = 0,
    )

    data class SkillXPInfo(
        var lastTotalXp: Float = 0f,
        var xpGainQueue: LinkedList<Float> = LinkedList(),
        var xpGainHour: Float = 0f,
        var xpGainLast: Float = 0f,
        var timer: Int = 3,
        var sessionTimerActive: Boolean = false,
        var isActive: Boolean = false,
        var lastUpdate: SimpleTimeMark = SimpleTimeMark.farPast(),
        var timeActive: Long = 0L,
        var shouldStartTimer: Boolean = true,
    )
}
