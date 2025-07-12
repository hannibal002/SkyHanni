package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.BitsApi
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.setLore
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.isStainedGlassPane
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import kotlin.reflect.KFunction

/**
 * Taken with permission from NotEnoughUpdates
 */
@SkyHanniModule
object OldSkyblockMenu {
    private val skyblockMenu = InventoryDetector { name -> name == "SkyBlock Menu" }
    private val storage get() = ProfileStorageData.profileSpecific?.maxwell
    private val enabled get() = SkyHanniMod.feature.inventory.oldSkyBlockMenu

    private fun isEnabled() = !RiftApi.inRift() && skyblockMenu.isInside() && enabled

    @HandleEvent(onlyOnSkyblock = true)
    fun onReplaceItem(event: ReplaceItemEvent) {
        if (!isEnabled()) return

        val sbButton = slotMap[event.slot] ?: return

        if (!event.originalItem.isStainedGlassPane()) {
            sbButton.disabled = true
            return ChatUtils.debug(
                "Skipping adding OldSkyBlockMenu item for button ${sbButton.name} at slot ${event.slot}," +
                    " because the original item is not a stained glass pane."
            )
        }

        val showWarning = sbButton.requiresBoosterCookie && !BitsApi.hasCookieBuff()
        val item = if (showWarning) sbButton.itemWithCookieWarning else sbButton.itemWithoutCookieWarning

        if (sbButton == SkyBlockButton.ACCESSORY) {
            val magicalPower = storage?.magicalPower ?: 0

            val lore = item.getLore().toMutableList()
            lore.add(4, "")
            val format = magicalPower.addSeparators()
            lore.add(5, "§7Magical Power: §6$format")

            val newItem = item.copy()
            newItem.setLore(lore)
            event.replace(newItem)
        } else event.replace(item)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return

        val sbButton = slotMap[event.slotId] ?: return
        if (sbButton.disabled) {
            return ChatUtils.debug("Button ${sbButton.name} at slot ${event.slotId} is disabled.")
        }

        event.cancel()

        val canClick = !sbButton.requiresBoosterCookie || BitsApi.hasCookieBuff()
        if (canClick) sbButton.command.call()
    }

    private val slotMap: Map<Int, SkyBlockButton> by lazy {
        SkyBlockButton.entries.associateBy { it.slot }
    }

    sealed interface ItemData
    class NormalItemData(val displayIcon: Item) : ItemData
    class SkullItemData(val uuid: String, val repoSkullId: String) : ItemData

    private enum class SkyBlockButton(
        val command: KFunction<Unit>,
        val slot: Int,
        private val displayName: String,
        private vararg val displayDescription: String,
        private val itemData: ItemData,
        val requiresBoosterCookie: Boolean = true,
        var disabled: Boolean = false,
    ) {
        TRADES(
            HypixelCommands::trades,
            40,
            "Trades",
            "View your available trades.",
            "These trades are always",
            "available and accessible through",
            "the SkyBlock Menu.",
            itemData = NormalItemData(Items.emerald),
            requiresBoosterCookie = false,
        ),
        ACCESSORY(
            HypixelCommands::accessories,
            53,
            "Accessory Bag",
            "A special bag which can hold",
            "Talismans, Rings, Artifacts, Relics, and",
            "Orbs within it. All will still",
            "work while in this bag!",
            itemData = SkullItemData("2b73dd76-5fc1-4ac3-8139-6a8992f8ce80", "SB_MENU_ACCESSORY"),
        ),
        POTION(
            HypixelCommands::potionBag,
            52,
            "Potion Bag",
            "A handy bag for holding your",
            "Potions in.",
            itemData = SkullItemData("991c4a18-3283-4629-b0fc-bbce23cd658c", "SB_MENU_POTION"),
        ),
        QUIVER(
            HypixelCommands::quiver,
            44,
            "Quiver",
            "A masterfully crafted Quiver",
            "which holds any kind of",
            "projectile you can think of!",
            itemData = SkullItemData("41758912-e6b1-4700-9de5-04f2cfb9c422", "SB_MENU_QUIVER"),
        ),
        FISHING(
            HypixelCommands::fishingBag,
            43,
            "Fishing Bag",
            "A useful bag which can hold all",
            "types of fish, baits, and fishing",
            "loot!",
            itemData = SkullItemData("508c01d6-eabe-430b-9811-874691ee7ee4", "SB_MENU_FISHING"),
        ),
        SACK_OF_SACKS(
            HypixelCommands::sacks,
            35,
            "Sack of Sacks",
            "A sack which contains other",
            "sacks. Sackception!",
            itemData = SkullItemData("a206a7eb-70fc-4f9f-8316-c3f69d6ba2ca", "SB_MENU_SACK_OF_SACKS"),
            requiresBoosterCookie = false,
        ),
        ;

        val itemWithCookieWarning: ItemStack by lazy { createItem(true) }
        val itemWithoutCookieWarning: ItemStack by lazy { createItem(false) }

        private fun buildLore(showCookieWarning: Boolean) = buildList {
            displayDescription.map { "§7$it" }.forEach { add(it) }
            add("")
            if (showCookieWarning) {
                add("§cYou need a booster cookie active")
                add("§cto use this shortcut!")
            } else add("§eClick to execute /${command.name.lowercase()}")
        }

        private fun createItem(showCookieWarning: Boolean): ItemStack {
            val name = "§a$displayName"
            val lore = buildLore(showCookieWarning)
            return when (itemData) {
                is NormalItemData -> ItemUtils.createItemStack(itemData.displayIcon, name, lore)
                is SkullItemData -> {
                    val skullTexture = SkullTextureHolder.getTexture(itemData.repoSkullId)
                    ItemUtils.createSkull(name, itemData.uuid, skullTexture, lore)
                }
            }
        }
    }
}
