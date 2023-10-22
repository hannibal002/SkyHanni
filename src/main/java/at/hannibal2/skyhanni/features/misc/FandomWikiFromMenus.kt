package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.anyContains
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FandomWikiFromMenus {

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "fandomWiki", "commands.fandomWiki")
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: SlotClickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return
        val chestName = InventoryUtils.openInventoryName()

        if (chestName.isEmpty()) return

        val itemClickedStack = event.slot.stack
        val itemClickedName = itemClickedStack.displayName
        val itemInHand = InventoryUtils.getItemInHand() ?: return
        val itemInHandName = itemInHand.nameWithEnchantment ?: return
        val internalName = itemInHand.getInternalName().asString() ?: return

        var wikiDisplayName = ""
        var wikiInternalName = ""

        if ((itemInHandName == "") || (event.slotId == 11 && itemClickedName.contains("Wiki Command") && chestName.contains("Wiki"))) {
            LorenzUtils.clickableChat("§e[SkyHanni] Click here to visit the Hypixel Skyblock Fandom Wiki!", "wiki")
            return
        } else if (event.slotId == 15 && itemClickedName.contains("Wikithis Command") && chestName.contains("Wiki")) {
            wikiDisplayName = itemInHandName
            wikiInternalName = internalName
        } else if ((itemClickedStack.getLore().anyContains("Wiki") || itemClickedStack.getLore().anyContains("wiki")) && !(itemClickedStack.getLore().anyContains("wikipedia"))) { //.lowercase() to match "Wiki!" and ".*wiki.*" lore lines in one fell swoop
            wikiDisplayName = itemClickedName.removeColor().replace("✔ ", "").replace("✖ ", "")
            wikiInternalName = itemClickedName.removeColor().replace("✔ ", "").replace("✖ ", "")
        } else return
        LorenzUtils.clickableChat("§e[SkyHanni] Click here to search for $wikiDisplayName §eon the Hypixel Skyblock Fandom Wiki!", "wiki $wikiInternalName")
        event.isCanceled = true

    }
    private fun isEnabled() = SkyHanniMod.feature.commands.fandomWiki.useFandomWiki
}
