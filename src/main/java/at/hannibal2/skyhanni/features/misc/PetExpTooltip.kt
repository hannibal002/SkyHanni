package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.indexOfFirst
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetExp
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PetExpTooltip {
    private val config get() = SkyHanniMod.feature.misc.petExperienceToolTip

    @SubscribeEvent(priority = EventPriority.LOWEST)
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
        val maxXp = maxPetExp(name) // lvl 100 legendary

        val percentage = petExperience / maxXp
        val percentageFormat = LorenzUtils.formatPercentage(percentage)

        event.toolTip.add(index, " ")
        if (percentage >= 1) {
            event.toolTip.add(index, "§7Total experience: §e${NumberUtil.format(petExperience)}")
        } else {
            val progressBar = StringUtils.progressBar(percentage)
            val isBelowLegendary = itemStack.getItemRarityOrNull()?.let { it < LorenzRarity.LEGENDARY } ?: false
            val addLegendaryColor = if (isBelowLegendary) "§6" else ""
            event.toolTip.add(index, "$progressBar §e${petExperience.addSeparators()}§6/§e${NumberUtil.format(maxXp)}")
            event.toolTip.add(index, "§7Progress to ${addLegendaryColor}Level $maxLevel: §e$percentageFormat")
        }
    }

    private fun maxPetExp(petName: String) = when {
        petName.contains("Golden Dragon") && config.goldenDragon200 -> 210_255_385 // lvl 200 legendary
        petName.contains("Bingo") -> 5_624_785 // lvl 100 common

        else -> 25_353_230 // lvl 100 legendary
    }
}
