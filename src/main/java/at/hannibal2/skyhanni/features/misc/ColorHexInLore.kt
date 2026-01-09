package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.MutableComponent

@SkyHanniModule
object ColorHexInLore {

    private val patternGroup = RepoPattern.group("color.item.hex.lore")

    /**
     * REGEX-TEST: #702963
     * REGEX-TEST: Hex #002FA7
     */
    private val hexPattern by patternGroup.pattern(
        "code",
        "(?:Hex )?(?<hex>#[0-9a-fA-F]{1,6})",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltip(event: ToolTipTextEvent) {
        if (!isEnabled()) return
        val itemCategory = event.itemStack.getItemCategoryOrNull()
        if (itemCategory != ItemCategory.DYE &&
            itemCategory !in ItemCategory.armor &&
            !InventoryUtils.openInventoryName().startsWith("Dye")
        ) return

        for (component in event.toolTip) {
            for (sibling in component.siblings) {
                hexPattern.matchMatcher(sibling) {
                    val hex = group("hex")
                    (sibling as MutableComponent).withColor(ColorUtils.getColorFromHex(hex))
                }
            }
        }
    }

    fun isEnabled() = SkyHanniMod.feature.inventory.hexAsColorInLore
}
