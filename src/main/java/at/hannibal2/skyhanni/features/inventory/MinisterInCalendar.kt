package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.setLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object MinisterInCalendar {

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!isEnabled()) return
        if (!MayorAPI.calendarGuiPattern.matches(InventoryUtils.openInventoryName())) return
        val minister = MayorAPI.currentMinister ?: return
        if (event.inventory is ContainerLocalMenu && event.slot == 38) {
            val item = "${minister.name}_MAYOR_MONSTER".asInternalName().getItemStack()

            val ministerColor = MayorAPI.mayorNameToColorCode(minister.mayorName)

            val ministerLore = buildList {
                add("§8(from SkyHanni)")
                add("")
                add("§8§m--------------------------")

                for (perk in minister.activePerks) {
                    add("$ministerColor${perk.perkName}")
                    add("§7${perk.description}")
                }

                add("§8§m--------------------------")
                add("")
                add("§7The Minister is who came in 2nd place")
                add("§7during the election. They have one")
                add("§7of their perks active.")
            }

            event.replace(
                item
                    .setLore(ministerLore)
                    .setStackDisplayName("${ministerColor}Minister ${minister.mayorName}"),
            )
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.inventory.ministerInCalendar
}
