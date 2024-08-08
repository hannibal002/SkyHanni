package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.item.ItemHoverEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.ExtendedChatColor
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object ColourHexInDyeMenu {

    private val patternGroup = RepoPattern.group("hex")
    /**
     * REGEX-TEST: §5§o§7to §4#960018§7!
     */
    private val hexPattern by patternGroup.pattern(
        "code",
        ".*(?<hex>#[0-9a-fA-F]{6}).*",
    )
    /**
     * REGEX-TEST: §5§o§7between §9#034150§7 and §9#009295§7!
     */
    private val doubleHexPattern by patternGroup.pattern(
        "code.animated",
        ".*(?<hexfirst>#[0-9a-fA-F]{6})§. and §.(?<hexsecond>#[0-9a-fA-F]{6})§.!",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun tooltip(event: ItemHoverEvent) {
        if (!SkyHanniMod.feature.inventory.dyeHexDisplay) return
        if (InventoryUtils.openInventoryName().startsWith("Dye")) {
            event.toolTip = event.toolTip.map {
                if (it.contains("Hex ")) {
                    it.split("Hex ").let { list ->
                        val s = list[1]
                        "Hex " + ExtendedChatColor(ColorUtils.getColorFromHex(s), false).toString() + s
                    }
                } else {
                    it
                }
            }.toMutableList()
        }
        val itemCategory = event.itemStack.getItemCategoryOrNull()
        if (itemCategory != ItemCategory.DYE) return
        event.toolTip = event.toolTip.map {
            doubleHexPattern.matchMatcher(it) {
                val group1 = group("hexfirst")
                val group2 = group("hexsecond")
                var newLine = it.replace(group1, ExtendedChatColor(ColorUtils.getColorFromHex(group1), false).toString() + group1)
                newLine = newLine.replace(group2, ExtendedChatColor(ColorUtils.getColorFromHex(group2), false).toString() + group2)
                newLine
            } ?: hexPattern.matchMatcher(it) {
                val group = group("hex")
                it.replace(group, ExtendedChatColor(ColorUtils.getColorFromHex(group), false).toString() + group)
            } ?: it

        }.toMutableList()
    }
}
