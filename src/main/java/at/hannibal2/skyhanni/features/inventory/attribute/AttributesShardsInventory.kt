package at.hannibal2.hanni.features.inventory.attribute

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.RenderInventoryItemTipEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.hanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.hanni.utils.RegexUtils.groupOrNull
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher

@HanniModule
object AttributesShardsInventory {

    private val config get() = AttributeShardsData.config

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!AttributeShardsData.attributeMenuInventory.isInside()) return
        if (!config.tierAsStackSize) return

        val internalName = event.stack.getInternalNameOrNull() ?: return
        if (!AttributeShardsData.isAttributeShard(internalName)) return
        AttributeShardsData.attributeShardNamePattern.matchMatcher(event.stack.displayName) {
            val tier = groupOrNull("tier")?.romanToDecimal() ?: 0
            val color = when (tier) {
                0 -> "§c"
                in 1..5 -> "§e"
                in 6..9 -> "§a"
                10 -> "§6"
                else -> "§f"
            }
            event.stackTip = "$color$tier"
        }
    }
}
