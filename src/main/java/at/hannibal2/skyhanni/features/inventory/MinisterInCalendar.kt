package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.events.item.ItemHoverEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches

@SkyHanniModule
object MinisterInCalendar {

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltip(event: ItemHoverEvent) {
        if (!SkyHanniMod.feature.inventory.ministerInCalendar) return
        if (!MayorAPI.calendarGuiPattern.matches(InventoryUtils.openInventoryName())) return
        if (!MayorAPI.mayorHeadPattern.matches(event.itemStack.displayName)) return
        val minister = MayorAPI.currentMinister ?: return
        val ministerColor = MayorAPI.mayorNameToColorCode(minister.mayorName)

        val ministerLore = buildList {
            add("${ministerColor}Minister ${minister.mayorName}")

            add("§8§m--------------------------")

            for (perk in minister.activePerks) {
                add("$ministerColor${perk.perkName}")
                add("§7${perk.description}")
            }

            add("§8§m--------------------------")
            add("")
        }

        event.toolTip.addAll(event.toolTip.size - 3, ministerLore)
    }

}
