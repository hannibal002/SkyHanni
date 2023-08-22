package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.indexOfFirst
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetExp
import at.hannibal2.skyhanni.utils.StringUtils
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PetExpTooltip {
    private val config get() = SkyHanniMod.feature.misc.petExperienceToolTip

    @SubscribeEvent
    fun onItemTooltipLow(event: ItemTooltipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.petDisplay) return
        if (!LorenzUtils.isShiftKeyDown() && !config.showAlways) return

        val itemStack = event.itemStack ?: return
        val petExperience = itemStack.getPetExp()?.round(1) ?: return
        val name = itemStack.name ?: return

        val index = event.toolTip.indexOfFirst(
            "§5§o§7§eClick to summon!",
            "§5§o§7§cClick to despawn!",
            "§5§o§7§eRight-click to add this pet to",
        ) ?: return

        val maxLevel = ItemUtils.maxPetLevel(name)
        val maxXp = if (maxLevel == 200) 210255385 else 25353230L

        val percentage = petExperience / maxXp
        val percentageFormat = LorenzUtils.formatPercentage(percentage)

        event.toolTip.add(index, " ")
        if (percentage >= 1) {
            event.toolTip.add(index, "§7Total experience: §e${NumberUtil.format(petExperience)}")
        } else {
            val progressBar = StringUtils.progressBar(percentage)
            event.toolTip.add(index, "$progressBar §e${petExperience.addSeparators()}§6/§e${NumberUtil.format(maxXp)}")
            event.toolTip.add(index, "§7Progress to Level $maxLevel: §e$percentageFormat")
        }
    }
}
