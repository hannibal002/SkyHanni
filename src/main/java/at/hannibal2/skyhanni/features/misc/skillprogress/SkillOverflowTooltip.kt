package at.hannibal2.skyhanni.features.misc.skillprogress

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
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SkillOverflowTooltip {

    private val config get() = SkyHanniMod.feature.misc.skillProgressConfig.overflowConfig

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!isEnabled()) return
        val inventoryName = InventoryUtils.openInventoryName()
        val stack = event.itemStack
        if (inventoryName == "Your Skills" && stack.getLore().any { it.contains("Click to view!") }) {
            val iterator = event.toolTip.listIterator()
            val skillName = stack.cleanName().split(" ").first().lowercase()
            val skillInfo = SkillAPI.skillMap?.get(skillName) ?: return
            for (line in iterator) {
                val maxReached = "§7§8Max Skill level reached!"
                if (line.contains(maxReached)) {
                    val progress = (skillInfo.overflowCurrentXp.toDouble() / skillInfo.overflowCurrentXpMax) * 100
                    val percent = "§e${progress.roundToPrecision(1)}%"
                    iterator.set("§7Progress to Level ${skillInfo.overflowLevel + 1}: $percent")
                    event.itemStack.name = "§a${skillName.firstLetterUppercase()} ${skillInfo.overflowLevel}"
                    continue
                }

                val bar = "                    "
                if (line.contains(bar)) {
                    val progress = (skillInfo.overflowCurrentXp.toDouble() / skillInfo.overflowCurrentXpMax)
                    val progressBar = StringUtils.progressBar(progress)
                    iterator.set("$progressBar §e${skillInfo.overflowCurrentXp.addSeparators()}§6/§e${skillInfo.overflowCurrentXpMax.addSeparators()}")
                    return
                }
            }
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enableInSkillMenuTooltip
}
