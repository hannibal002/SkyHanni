package at.hannibal2.skyhanni.utils

import com.google.common.base.Splitter
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.Locale
import java.util.regex.Pattern

class XPInformation {
    class SkillInfo {
        var level = 0
        var totalXp = 0f
        var currentXp = 0f
        var currentXpMax = 0f
        var fromApi = false
    }

    val skillInfoMap = HashMap<String, SkillInfo>()
    var updateWithPercentage = HashMap<String, Float>()
    var correctionCounter = 0
    fun getSkillInfo(skillName: String): SkillInfo? {
        return skillInfoMap[skillName.lowercase(Locale.getDefault())]
    }

    private var lastActionBar: String? = null
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    fun onChatReceived(event: ClientChatReceivedEvent) {
//        if (event.type.toInt() == 2) {
//            val leveling = Constants.LEVELING ?: return
//            val actionBar = StringUtils.cleanColour(event.message.unformattedText)
//            if (lastActionBar != null && lastActionBar.equals(actionBar, ignoreCase = true)) {
//                return
//            }
//            lastActionBar = actionBar
//            val components = SPACE_SPLITTER.splitToList(actionBar)
//            for (component in components) {
//                var matcher = SKILL_PATTERN.matcher(component)
//                if (matcher.matches()) {
//                    val skillS = matcher.group(2)
//                    val currentXpS = matcher.group(3).replace(",", "")
//                    val maxXpS = matcher.group(4).replace(",", "")
//                    val currentXp = currentXpS.toFloat()
//                    val maxXp = maxXpS.toFloat()
//                    val skillInfo = SkillInfo()
//                    skillInfo.currentXp = currentXp
//                    skillInfo.currentXpMax = maxXp
//                    skillInfo.totalXp = currentXp
//                    val levelingArray = leveling.getAsJsonArray("leveling_xp")
//                    for (i in 0 until levelingArray.size()) {
//                        val cap = levelingArray[i].asFloat
//                        if (maxXp > 0 && maxXp <= cap) {
//                            break
//                        }
//                        skillInfo.totalXp += cap
//                        skillInfo.level++
//                    }
//                    skillInfoMap[skillS.lowercase(Locale.getDefault())] = skillInfo
//                    return
//                } else {
//                    matcher = SKILL_PATTERN_PERCENTAGE.matcher(component)
//                    if (matcher.matches()) {
//                        val skillS = matcher.group(2)
//                        val xpPercentageS = matcher.group(3).replace(",", "")
//                        val xpPercentage = xpPercentageS.toFloat()
//                        updateWithPercentage[skillS.lowercase(Locale.getDefault())] = xpPercentage
//                    } else {
//                        matcher = SKILL_PATTERN_MULTIPLIER.matcher(component)
//                        if (matcher.matches()) {
//                            val skillS = matcher.group(2)
//                            val currentXpS = matcher.group(3).replace(",", "")
//                            var maxXpS = matcher.group(4).replace(",", "")
//                            var maxMult = 1f
//                            if (maxXpS.endsWith("k")) {
//                                maxMult = 1000f
//                                maxXpS = maxXpS.substring(0, maxXpS.length - 1)
//                            } else if (maxXpS.endsWith("m")) {
//                                maxMult = 1000000f
//                                maxXpS = maxXpS.substring(0, maxXpS.length - 1)
//                            } else if (maxXpS.endsWith("b")) {
//                                maxMult = 1000000000f
//                                maxXpS = maxXpS.substring(0, maxXpS.length - 1)
//                            }
//                            val currentXp = currentXpS.toFloat()
//                            val maxXp = maxXpS.toFloat() * maxMult
//                            val skillInfo = SkillInfo()
//                            skillInfo.currentXp = currentXp
//                            skillInfo.currentXpMax = maxXp
//                            skillInfo.totalXp = currentXp
//                            val levelingArray = leveling.getAsJsonArray("leveling_xp")
//                            for (i in 0 until levelingArray.size()) {
//                                val cap = levelingArray[i].asFloat
//                                if (maxXp > 0 && maxXp <= cap) {
//                                    break
//                                }
//                                skillInfo.totalXp += cap
//                                skillInfo.level++
//                            }
//                            skillInfoMap[skillS.lowercase(Locale.getDefault())] = skillInfo
//                            return
//                        }
//                    }
//                }
//            }
//        }
    }

    fun updateLevel(skill: String, level: Int) {
//        if (updateWithPercentage.containsKey(skill)) {
//            val leveling = Constants.LEVELING ?: return
//            val skillInfo = SkillInfo()
//            skillInfo.totalXp = 0f
//            skillInfo.level = level
//            val levelingArray = leveling.getAsJsonArray("leveling_xp")
//            for (i in 0 until levelingArray.size()) {
//                val cap = levelingArray[i].asFloat
//                if (i == level) {
//                    skillInfo.currentXp += updateWithPercentage[skill]!! / 100f * cap
//                    skillInfo.totalXp += skillInfo.currentXp
//                    skillInfo.currentXpMax = cap
//                    break
//                } else {
//                    skillInfo.totalXp += cap
//                }
//            }
//            val old = skillInfoMap[skill.lowercase(Locale.getDefault())]
//            if (old!!.totalXp <= skillInfo.totalXp) {
//                correctionCounter--
//                if (correctionCounter < 0) correctionCounter = 0
//                skillInfoMap[skill.lowercase(Locale.getDefault())] = skillInfo
//            } else if (++correctionCounter >= 10) {
//                correctionCounter = 0
//                skillInfoMap[skill.lowercase(Locale.getDefault())] = skillInfo
//            }
//        }
        updateWithPercentage.clear()
    }

    fun tick() {
//        ProfileApiSyncer.getInstance().requestResync("xpinformation", (5 * 60 * 1000).toLong(),
//            {}
//        ) { profile: ProfileViewer.Profile ->
//            onApiUpdated(
//                profile
//            )
//        }
    }

//    private fun onApiUpdated(profile: ProfileViewer.Profile) {
//        val skyblockInfo = profile.getSkyblockInfo(null)
//        for (skill in skills) {
//            val info = SkillInfo()
//            val levelInfo = skyblockInfo[skill]
//            val level = levelInfo!!.level
//            info.totalXp = levelInfo.totalXp
//            info.currentXpMax = levelInfo.maxXpForLevel
//            info.level = level.toInt()
//            info.currentXp = level % 1 * info.currentXpMax
//            info.fromApi = true
//            skillInfoMap[skill.lowercase(Locale.getDefault())] = info
//        }
//    }

    companion object {
        val instance = XPInformation()
        private val SPACE_SPLITTER = Splitter.on("  ").omitEmptyStrings().trimResults()
        private val SKILL_PATTERN = Pattern.compile(
            "\\+(\\d+(?:,\\d+)*(?:\\.\\d+)?) (.+) \\((\\d+(?:,\\d+)*(?:\\.\\d+)?)/(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\)"
        )
        private val SKILL_PATTERN_MULTIPLIER =
            Pattern.compile("\\+(\\d+(?:,\\d+)*(?:\\.\\d+)?) (.+) \\((\\d+(?:,\\d+)*(?:\\.\\d+)?)/(\\d+(?:k|m|b))\\)")
        private val SKILL_PATTERN_PERCENTAGE =
            Pattern.compile("\\+(\\d+(?:,\\d+)*(?:\\.\\d+)?) (.+) \\((\\d\\d?(?:\\.\\d\\d?)?)%\\)")
        private val skills = arrayOf(
            "taming",
            "mining",
            "foraging",
            "enchanting",
            "carpentry",
            "farming",
            "combat",
            "fishing",
            "alchemy",
            "runecrafting"
        )
    }
}

