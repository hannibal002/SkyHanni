package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.isTopInventory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object FocusMode {

    private val config get() = SkyHanniMod.feature.inventory.focusMode

    private var active = false
    private var inAuctionHouse = false

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onTooltip(event: ToolTipTextEvent) {
        if (!isEnabled()) return
        if (event.toolTip.isEmpty()) return
        if (config.hideMenuItems) {
            event.itemStack.getInternalNameOrNull().let {
                if (it == null || it == "SKYBLOCK_MENU".toInternalName()) return
            }
            val inBazaar = BazaarApi.inBazaarInventory && event.slot?.isTopInventory() == true
            if (inBazaar) return
        }

        val keyName = KeyboardManager.getKeyName(config.toggleKey)

        val hint = !config.disableHint && !config.alwaysEnabled && keyName != "NONE"
        if (active || config.alwaysEnabled) {
            val newTooltip = buildList {
                add(event.toolTip.first())
                if (hint) {
                    add("§7Focus Mode from SkyHanni active!".asComponent())
                    add("§7Press $keyName to disable!".asComponent())
                }
                val separator = "-----------------"
                if (inAuctionHouse) {
                    var index = -1
                    for ((i, component) in event.toolTip.withIndex()) {
                        if (component.string.contains(separator)) {
                            index = i
                            break
                        }
                    }
                    if (index > -1) {
                        val ahLore = event.toolTip.drop(index).take(20)
                        addAll(ahLore)
                    }
                }
            }.toMutableList()
            event.toolTip.clear()
            event.toolTip.addAll(newTooltip)
        } else {
            if (hint) {
                event.toolTip.add(1, "§7Press $keyName to enable Focus Mode from SkyHanni!")
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
