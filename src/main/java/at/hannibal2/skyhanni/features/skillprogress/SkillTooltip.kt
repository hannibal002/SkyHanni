package at.hannibal2.skyhanni.features.skillprogress

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.SkillAPI
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.NumberUtil.toRoman
import at.hannibal2.skyhanni.utils.StringUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SkillTooltip {

    private val config get() = SkyHanniMod.feature.skillProgress
    private val overflowConfig get() = config.overflowConfig
    private val customGoalConfig get() = config.customGoalConfig

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val inventoryName = InventoryUtils.openInventoryName()
        val stack = event.itemStack
        if (inventoryName == "Your Skills" && stack.getLore().any { it.contains("Click to view!") }) {
            val iterator = event.toolTip.listIterator()
            val split = stack.cleanName().split(" ")
            val skillName = split.first()
            val skill = SkillType.getByNameOrNull(skillName) ?: return
            val useRoman = split.last().toIntOrNull() == null
            val skillInfo = SkillAPI.storage?.get(skill) ?: return
            val showCustomGoal = skillInfo.customGoalLevel != 0 && customGoalConfig.enableInSkillMenuTooltip
            var next = false
            for (line in iterator) {
                val maxReached = "§7§8Max Skill level reached!"
                if (line.contains(maxReached) && overflowConfig.enableInSkillMenuTooltip) {
                    val progress = (skillInfo.overflowCurrentXp.toDouble() / skillInfo.overflowCurrentXpMax) * 100
                    val percent = "§e${progress.roundToPrecision(1)}%"
                    val currentLevel = skillInfo.overflowLevel

                    val level = if (useRoman) currentLevel.toRoman() else currentLevel
                    val nextLevel = if (useRoman) (currentLevel + 1).toRoman() else currentLevel + 1
                    iterator.set("§7Progress to Level $nextLevel: $percent")

                    event.itemStack.name = "§a${skill.displayName} $level"
                    next = true
                    continue
                }
                val bar = "                    "
                if (next && overflowConfig.enableInSkillMenuTooltip) {
                    if (line.contains(bar)) {
                        val progress = (skillInfo.overflowCurrentXp.toDouble() / skillInfo.overflowCurrentXpMax)
                        val progressBar = StringUtils.progressBar(progress)
                        iterator.set("$progressBar §e${skillInfo.overflowCurrentXp.addSeparators()}§6/§e${skillInfo.overflowCurrentXpMax.addSeparators()}")
                        iterator.add("")
                    }
                }
                if ((line.contains(bar) || line.contains("/")) && showCustomGoal) {
                    val targetLevel = skillInfo.customGoalLevel
                    var have = skillInfo.overflowTotalXp
                    val need = SkillUtil.xpRequiredForLevel(targetLevel.toDouble())
                    if (targetLevel in 50 .. 60 && skillInfo.overflowLevel >= 50) have += SkillUtil.xpRequiredForLevel(50.0)
                    else if (targetLevel > 60 && skillInfo.overflowLevel >= 60) have += SkillUtil.xpRequiredForLevel(60.0)
                    val progress = have.toDouble() / need
                    val progressBar = StringUtils.progressBar(progress)
                    val nextLevel = if (useRoman) targetLevel.toRoman() else targetLevel
                    val percent = "§e${(progress * 100).roundToPrecision(1)}%"
                    iterator.add("")
                    iterator.add("§7Progress to Level $nextLevel: $percent")
                    iterator.add("$progressBar §e${have.addSeparators()}§6/§e${need.addSeparators()}")
                    iterator.add("")
                }
                if (next && overflowConfig.enableInSkillMenuTooltip) {
                    if (line.contains(bar)) {
                        iterator.add("§b§lOVERFLOW XP:")
                        iterator.add("§7▸ ${skillInfo.overflowTotalXp.addSeparators()}")
                    }
                }
            }
        }
    }
}