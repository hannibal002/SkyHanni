package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ExtendedChatColor
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component

@SkyHanniModule
object ColorHexInLore {

    private val patternGroup = RepoPattern.group("color.item.hex.lore")

    /**
     * REGEX-TEST: §5§o§7to §4#960018§7!
     * REGEX-TEST: §8Hex #F56FA1
     * REGEX-TEST: Color: #1793C4
     */
    private val hexPattern by patternGroup.pattern(
        "code",
        ".*(?:Color:|Hex|to) (?:§.)?(?<hex>#[0-9a-fA-F]{1,6}).*",
    )

    /**
     * REGEX-TEST: §5§o§7between §9#034150§7 and §9#009295§7!
     */
    private val doubleHexPattern by patternGroup.pattern(
        "code.animated",
        ".*(?<hexfirst>#[0-9a-fA-F]{6})§. and §.(?<hexsecond>#[0-9a-fA-F]{6})§.!",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltip(event: ToolTipTextEvent) {
        // this feature wont work on 1.21 probably until we drop 1.8
        // todo actually fix now
        if (true) return
        if (!isEnabled()) return
        val itemCategory = event.itemStack.getItemCategoryOrNull()
        if (itemCategory != ItemCategory.DYE &&
            itemCategory !in ItemCategory.armor &&
            !InventoryUtils.openInventoryName().startsWith("Dye")
        ) return

        for ((index, component) in event.toolTip.withIndex()) {
            for (sibling in component.siblings) {
                // ill fix it in a separate pr
                if (sibling.string.contains("#")) {} // blah blah do something
            }
        }
    }

    private fun addColor(hexFirst: String): Component = ExtendedChatColor(hexFirst).asText(hexFirst)

    fun isEnabled() = SkyBlockUtils.inSkyBlock && SkyHanniMod.feature.inventory.hexAsColorInLore
}
