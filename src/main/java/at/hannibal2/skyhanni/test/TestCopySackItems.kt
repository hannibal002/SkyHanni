package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.SackApi
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.OSUtils

@SkyHanniModule
object TestCopySackItems {

    @HandleEvent
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!DevApi.config.debug.copySacksItems) return
        if (!SackApi.inventory.isInside()) return

        val sackItems = mutableSetOf<String>()
        for (item in event.inventoryItems.values) {
            val internalName = item.getInternalNameOrNull() ?: continue
            sackItems.add(internalName.asString())
        }
        if (sackItems.isEmpty()) return

        OSUtils.copyToClipboard(ConfigManager.gson.toJson(sackItems))
        ChatUtils.debug("${sackItems.size} items saved to clipboard.")
    }
}
