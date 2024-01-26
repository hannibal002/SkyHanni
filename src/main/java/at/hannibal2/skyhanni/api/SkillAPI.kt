package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.config.Storage.ProfileSpecific
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.common.base.Splitter
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import io.github.moulberry.notenoughupdates.util.Constants
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.io.Serializable

object SkillAPI {
    private val patternGroup = RepoPattern.group("skilldisplay")
    private val SKILL_PATTERN_PERCENT by patternGroup.pattern("skillpaternpercent", "\\+(\\d+(?:,\\d+)*(?:\\.\\d+)?) (.+) \\((\\d\\d?(?:\\.\\d\\d?)?)%\\)")
    private val SKILL_PATTERN by patternGroup.pattern("skillpattern", "\\+(\\d+(?:,\\d+)*(?:\\.\\d+)?) (.+) \\((\\d+(?:,\\d+)*(?:\\.\\d+)?)/(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\)")
    private val SKILL_PATTERN_MULTIPLIER by patternGroup.pattern("skillpatternmultiplier", "\\+(\\d+(?:,\\d+)*(?:\\.\\d+)?) (.+) \\((\\d+(?:,\\d+)*(?:\\.\\d+)?)/(\\d+(?:.\\d+)*(?:k|m|b))\\)")
    private val skillTabPattern by patternGroup.pattern("skilltabpattern", "^§e§lSkills: §r§a(?<type>\\w+) (?<level>\\d+): §r§3(?<progress>.+)%\$")
    private val maxSkillTabPattern by patternGroup.pattern("maxskilltabpattern", "^§e§lSkills: §r§a(?<type>\\w+) (?<level>\\d+): §r§c§lMAX\$")
    private val SPACE_SPLITTER = Splitter.on("  ").omitEmptyStrings().trimResults()
    private var lastActionBar: String? = null
    private val levelingMap = mutableMapOf<Int, Int>()
    val skillMap: MutableMap<String, SkillInfo>? get() = ProfileStorageData.profileSpecific?.skillMap

    var activeSkill = ""
    var gained = ""
    var showDisplay = false
    var lastUpdate = SimpleTimeMark.farPast()

    /**
     * Taken and modified from NotEnoughUpdates under Creative Commons Attribution-NonCommercial 3.0
     * https://github.com/Moulberry/NotEnoughUpdates/blob/master/LICENSE
     * @author Moulberry
     */
    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        val actionBar = event.message.removeColor()

        if (lastActionBar != null && lastActionBar == actionBar) return
        lastActionBar = actionBar

        val components = SPACE_SPLITTER.splitToList(actionBar)
        for (component in components) {
            var matcher = SKILL_PATTERN.matcher(component)
            if (matcher.matches()) {
                gained = matcher.group(1)
                val skillS = matcher.group(2)
                val currentXpS = matcher.group(3).replace(",", "")
                val maxXpS = matcher.group(4).replace(",", "")

                val currentXp = currentXpS.toFloat()
                val maxXp = maxXpS.toFloat()
                val skillInfo = SkillInfo()
                val level = getLevel(maxXp.toLong())
                val (levelOverflow, currentOverflow, currentMaxOverflow, totalOverflow) = getSkillInfo(level, currentXp.toLong(), maxXp.toLong(), maxXp.toLong())
                skillInfo.currentXp = currentOverflow.toFloat()
                skillInfo.currentXpMax = currentMaxOverflow.toFloat()
                skillInfo.level = levelOverflow
                skillInfo.totalXp = totalOverflow.toFloat()
                //skillMap = skillMap.editCopy { this[skillS.lowercase()] = skillInfo }
                val lowerSkill = skillS.lowercase()
                skillMap?.set(lowerSkill, skillInfo)
                activeSkill = lowerSkill
                lastUpdate = SimpleTimeMark.now()
                showDisplay = true
                return
            } else {
                matcher = SKILL_PATTERN_PERCENT.matcher(component)
                if (matcher.matches()) {
                    gained = matcher.group(1)
                    val skillS = matcher.group(2)
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

                    val existingLevel = getSkillInfo(skillS.lowercase()) ?: SkillInfo()
                    val xpPercentageS = matcher.group(3).replace(",", "")
                    val xpPercentage = xpPercentageS.toFloatOrNull() ?: return
                    val levelingArray = levelArray(skillS.lowercase())
                    val levelXp = calculateLevelXp(levelingArray, existingLevel.level - 1)
                    val nextLevelDiff = levelingArray[tablistLevel].asDouble
                    val nextLevelProgress = nextLevelDiff * xpPercentage / 100
                    val totalXp = levelXp + nextLevelProgress
                    val (_, currentOverflow, currentMaxOverflow, totalOverflow) = getSkillInfo(tablistLevel, nextLevelProgress.toLong(), nextLevelDiff.toLong(), totalXp.toLong())

                    existingLevel.totalXp = totalOverflow.toFloat()
                    existingLevel.currentXp = currentOverflow.toFloat()
                    existingLevel.currentXpMax = currentMaxOverflow.toFloat()
                    existingLevel.level = tablistLevel
                   // skillMap = skillMap.editCopy { this[skillS.lowercase()] = existingLevel }
                    val lowerSkill = skillS.lowercase()
                    skillMap?.set(lowerSkill, existingLevel)
                    activeSkill = lowerSkill
                    lastUpdate = SimpleTimeMark.now()
                    showDisplay = true
                } else {
                    matcher = SKILL_PATTERN_MULTIPLIER.matcher(component)
                    if (matcher.matches()) {
                        gained = matcher.group(1)
                        val skillS = matcher.group(2)
                        val currentXpS = matcher.group(3).replace(",", "")
                        var maxXpS = matcher.group(4).replace(",", "")
                        var maxMult = 1f
                        if (maxXpS.endsWith("k")) {
                            maxMult = 1000f
                            maxXpS = maxXpS.substring(0, maxXpS.length - 1)
                        } else if (maxXpS.endsWith("m")) {
                            maxMult = 1000000f
                            maxXpS = maxXpS.substring(0, maxXpS.length - 1)
                        } else if (maxXpS.endsWith("b")) {
                            maxMult = 1000000000f
                            maxXpS = maxXpS.substring(0, maxXpS.length - 1)
                        }
                        val currentXp = currentXpS.toFloat()
                        val maxXp = maxXpS.toFloat() * maxMult

                        val skillInfo = SkillInfo()
                        val level = getLevel(maxXp.toLong())
                        val (currentLevel, currentOverflow, currentMaxOverflow, totalOverflow) = getSkillInfo(level, currentXp.toLong(), maxXp.toLong(), maxXp.toLong())

                        skillInfo.currentXp = currentOverflow.toFloat()
                        skillInfo.currentXpMax = currentMaxOverflow.toFloat()
                        skillInfo.totalXp = totalOverflow.toFloat()
                        skillInfo.level = currentLevel
                       // skillMap = skillMap.editCopy { this[skillS.lowercase()] = skillInfo }
                        val lowerSkill = skillS.lowercase()
                        skillMap?.set(lowerSkill, skillInfo)
                        activeSkill = lowerSkill
                        lastUpdate = SimpleTimeMark.now()
                        showDisplay = true
                        return
                    }
                }
            }
        }
    }

    fun getSkillInfo(skillName: String): SkillInfo? {
        return skillMap?.get(skillName)
    }

    fun getSkillInfo(currentLevel: Int, currentXp: Long, neededXp: Long, totalXp: Long): Quad<Int, Long, Long, Long> {
        val (level, overflowExp, xpForCurrentLevel, total) = calculateOverFlow(currentXp)
        return if (currentLevel >= 60 && SkyHanniMod.feature.misc.skillProgressDisplayConfig.showOverflow.get()) Quad(level, overflowExp, xpForCurrentLevel, total) else Quad(currentLevel, currentXp, neededXp, totalXp)
    }

    private fun calculateOverFlow(currentXp: Long): Quad<Int, Long, Long, Long> {
        var overflowExp = currentXp
        var level = 60
        var slope = 600000L
        var xpForCurrentLevel = 7000000L
        var total = 0L
        while (overflowExp > xpForCurrentLevel) {
            level += 1
            overflowExp -= xpForCurrentLevel
            total += xpForCurrentLevel
            xpForCurrentLevel += slope
            if (level % 10 == 0)
                slope *= 2

        }
        total += overflowExp
        return Quad(level, overflowExp, xpForCurrentLevel, total)
    }

    private fun getLevel(neededXp: Long): Int {
        val map: List<Int> = Gson().fromJson(Utils.getElement(Constants.LEVELING, "leveling_xp").asJsonArray.toString(), object : TypeToken<List<Int>>() {}.type)
        for ((i, e) in map.withIndex()) {
            levelingMap[e] = i
        }
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

    data class SkillInfo(var level: Int = 0, var totalXp: Float = 0f, var currentXp: Float = 0f, var currentXpMax: Float = 0f)

    data class Quad<out A, out B, out C, out D>(
        val first: A,
        val second: B,
        val third: C,
        val quad: D
    ) : Serializable {
        override fun toString(): String = "($first, $second, $third, $quad)"
    }
}
