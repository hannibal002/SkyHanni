package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object TestCopySackItems {

    private val patternGroup = RepoPattern.group("test.dev.copysacks")

    /**
     * REGEX-TEST: Large Combat Sack
     */
    private val inventoryNamePattern by patternGroup.pattern(
        "inventory",
        ".* Sack",
    )

    val inventory = InventoryDetector { inventoryNamePattern }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!DevApi.config.debug.copySacksItems) return
        if (!inventory.isInside()) return

        val sackItems = mutableSetOf<String>()
        for (item in event.inventoryItems.values) {
            val internalName = item.getInternalName()
            if (internalName == NeuInternalName.NONE) continue
            sackItems.add(internalName.asString())
        }
        if (sackItems.isEmpty()) return

        OSUtils.copyToClipboard(ConfigManager.gson.toJson(sackItems))
        ChatUtils.debug("${sackItems.size} items saved to clipboard.")
    }
}
