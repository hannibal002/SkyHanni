package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.Mayor
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.setLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.splitLines
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object MinisterInCalendar {

    private const val MINISTER_SLOT = 38
    private var ministerItemStack: ItemStack? = null

    private val prefix = listOf(
        "§8(from SkyHanni)",
        "",
        "§8§m--------------------------",
    )
    private val suffix = listOf(
        "§8§m--------------------------",
        "",
        "§7The Minister is who came in 2nd place",
        "§7during the election. They have one",
        "§7of their perks active.",
    )

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        if (!MayorAPI.calendarGuiPattern.matches(InventoryUtils.openInventoryName())) return
        val minister = MayorAPI.currentMinister ?: return

        val itemStack = "${minister.name}_MAYOR_MONSTER".asInternalName().getItemStack()
        val ministerColor = MayorAPI.mayorNameToColorCode(minister.mayorName)

        ministerItemStack = changeItem(ministerColor, minister, itemStack)
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (!isEnabled()) return
        if (!MayorAPI.calendarGuiPattern.matches(InventoryUtils.openInventoryName())) return
        ministerItemStack = null
    }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!isEnabled()) return
        if (event.inventory !is ContainerLocalMenu || event.slot != MINISTER_SLOT) return
        if (!MayorAPI.calendarGuiPattern.matches(InventoryUtils.openInventoryName())) return
        event.replace(ministerItemStack ?: return)
    }

    private fun changeItem(
        ministerColor: String,
        minister: Mayor,
        item: ItemStack,
    ): ItemStack? {
        val ministerDisplayName = "${ministerColor}Minister ${minister.mayorName}"
        val ministerLore = buildList {
            addAll(prefix)
            for (perk in minister.activePerks) {
                add("$ministerColor${perk.perkName}")
                addAll(perk.description.splitLines(170).removeResets().split("\n").map { "§7$it" })
            }
            addAll(suffix)
        }

        return item.setLore(ministerLore).setStackDisplayName(ministerDisplayName)
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.inventory.ministerInCalendar
}
