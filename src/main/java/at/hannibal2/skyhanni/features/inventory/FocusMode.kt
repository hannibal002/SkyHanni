package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.InventoryOpenEvent
import at.hannibal2.hanni.events.minecraft.KeyDownEvent
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.InventoryUtils.isTopInventory
import at.hannibal2.hanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.hanni.utils.KeyboardManager
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.collection.CollectionUtils.sublistAfter

@HanniModule
object FocusMode {

    private val config get() = HanniMod.feature.inventory.focusMode

    private var active = false
    private var inAuctionHouse = false

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onTooltip(event: ToolTipEvent) {
        if (!isEnabled()) return
        if (event.toolTip.isEmpty()) return
        if (config.hideMenuItems) {
            event.itemStack.getInternalNameOrNull().let {
                if (it == null || it == "SKYBLOCK_MENU".toInternalName()) return
            }
            val inBazaar = BazaarApi.inBazaarInventory && event.slot.isTopInventory()
            if (inBazaar) return
        }

        val keyName = KeyboardManager.getKeyName(config.toggleKey)

        val hint = !config.disableHint && !config.alwaysEnabled && keyName != "NONE"
        if (active || config.alwaysEnabled) {
            event.toolTip = buildList {
                add(event.toolTip.first())
                if (hint) {
                    add("§7Focus Mode from Hanni active!")
                    add("Press $keyName to disable!")
                }
                val separator = "§5§o§8§m-----------------"
                if (inAuctionHouse && event.toolTip.contains(separator)) {
                    val ahLore = event.toolTip.sublistAfter(separator, amount = 20)
                    add(separator)
                    addAll(ahLore)
                }
            }.toMutableList()
        } else {
            if (hint) {
                event.toolTip.add(1, "§7Press $keyName to enable Focus Mode from Hanni!")
            }
        }
    }

    @HandleEvent
    fun onKeyDown(event: KeyDownEvent) {
        if (!isEnabled()) return
        if (config.alwaysEnabled) return
        if (event.keyCode != config.toggleKey) return
        active = !active
    }

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        inAuctionHouse = event.inventoryName.startsWith("Auctions")
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && InventoryUtils.inContainer() && config.enabled
}
