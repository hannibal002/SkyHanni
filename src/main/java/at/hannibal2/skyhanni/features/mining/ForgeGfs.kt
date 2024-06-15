package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.util.stripControlCodes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ForgeGfs {

    private val patternGroup = RepoPattern.group("mining.forge")

    private val forgePattern by patternGroup.pattern(
        "inventory.name",
        "^(Refine|Item Casting) \\(Slot #\\d+\\)\$"
    )

    // Matching Gemstone: ^[\p{L}\p{S}\p{P}\s]+ x\d+$|^\d{1,3}(,\d{3})* Coins$
    // Old: ^[A-Za-z\s'-]+ x\d+$|^\d{1,3}(,\d{3})* Coins$
    private val ingredientPattern by patternGroup.pattern(
        "ingredient",
        "^[\\p{L}\\p{S}\\p{P}\\s]+ x\\d+\$|^\\d{1,3}(,\\d{3})* Coins\$"
    )

    private val config get() = SkyHanniMod.feature.mining.forgeGfs

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config) return
        if (!forgePattern.matches(InventoryUtils.openInventoryName())) return
        if (!event.clickTypeEnum?.equals(GuiContainerEvent.ClickType.SHIFT)!!) return

        val itemLore = event.item?.getLore()
        val itemLorePlain = itemLore?.map {it.stripControlCodes()}
        // ChatUtils.debug("Item lore: ${itemLore.toString()}")
        // ChatUtils.debug("Item lore plain: ${itemLorePlain.toString()}")

        val matchedLines = itemLorePlain?.filter {ingredientPattern.matches(it)}
        ChatUtils.debug("Matched lines: ${matchedLines.toString()}")

    }
}
