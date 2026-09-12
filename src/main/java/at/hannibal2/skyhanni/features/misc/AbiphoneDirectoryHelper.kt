package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.commands.WikiManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.takeUnlessBlank
import at.hannibal2.skyhanni.utils.RegexUtils.matches

@SkyHanniModule
object AbiphoneDirectoryHelper {

    private val config get() = SkyHanniMod.feature.misc.abiphoneDirectoryHelper

    /**
     * REGEX-TEST: Contacts Directory
     * REGEX-TEST: (1/2) Contacts Directory
     */
    private val inventoryPattern by AbiphoneFeatures.patternGroup.pattern(
        "inventory",
        "(?:\\(\\d+/\\d+\\) )?Contacts Directory",
    )

    private var inContactsDirectory = false

    @HandleEvent
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (!inventoryPattern.matches(event.inventoryName)) return
        inContactsDirectory = true
    }

    @HandleEvent
    private fun onInventoryClose(event: InventoryCloseEvent) {
        if (!isEnabled()) return
        inContactsDirectory = false
    }

    @HandleEvent
    private fun onGuiClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        if (!config.wikiOnClick) return
        if (!inContactsDirectory) return
        val slot = event.slot ?: return
        if (slot.index !in 10..43) return
        val stack = slot.item.takeUnlessBlank() ?: return

        val name = stack.cleanName
        WikiManager.sendWikiMessage(name, autoOpen = config.autoOpenWiki)
        event.cancel()
    }

    private fun isEnabled() = config.enabled
}
