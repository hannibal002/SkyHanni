package at.hannibal2.skyhanni.features.anvil

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.InventoryUtils.getLowerItems
import at.hannibal2.skyhanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AnvilCombineHelper {

    private val patternGroup = RepoPattern.group("anvil.helper")
    private val guiNamePattern by patternGroup.pattern(
        "guiname",
        "Anvil"
    )

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.inventory.anvilCombineHelper) return

        if (event.gui !is GuiChest) return
        val chest = event.gui.inventorySlots as ContainerChest
        val chestName = chest.getInventoryName()

        if (!guiNamePattern.matches(chestName)) return

        val matchLore = mutableListOf<String>()

        for ((slot, stack) in chest.getUpperItems()) {
            if (slot.slotNumber == 29) {
                val lore = stack.getLore()
                matchLore.addAll(lore)
                break
            }
        }

        if (matchLore.isEmpty()) return

        for ((slot, stack) in chest.getLowerItems()) {
            if (matchLore == stack.getLore()) {
                slot highlight LorenzColor.GREEN
            }
        }
    }
}
