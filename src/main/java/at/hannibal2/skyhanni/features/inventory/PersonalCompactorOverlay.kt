package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.PersonalCompactorConfig
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems.getInternalNameFromHypixelId
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getAttributeString
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableInventory
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PersonalCompactorOverlay {

    private val config get() = SkyHanniMod.feature.inventory.personalCompactor

    private val group = RepoPattern.group("inventory.personalcompactor")
    private val internalNamePattern by group.pattern(
        "internalname",
        "PERSONAL_(?<type>[^_]+)_(?<tier>\\d+)",
    )

    private val slotsMap = mapOf(
        7000 to 12,
        6000 to 7,
        5000 to 3,
        4000 to 1
    )

    private const val MAX_ITEMS_PER_ROW = 7

    private val compactorMap = mutableMapOf<String, Renderable>()

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!isEnabled()) return
        if (!shouldShow()) return

        val itemStack = event.itemStack
        val internalName = itemStack.getInternalName()
        val name = event.toolTip.firstOrNull() ?: return
        val (type, tier) = internalNamePattern.matchMatcher(internalName.asString()) {
            group("type") to group("tier").formatInt()
        } ?: return
        // NEU using getTooltip triggers LorenzToolTipEvent, which makes it use the tooltip of a different item
        // but the itemstack of a different one. this is a workaround until this issue is fixed
        if (!name.contains("Personal")) return

        val prefix = when (type) {
            "COMPACTOR" -> "personal_compact_"
            "DELETOR" -> "personal_deletor_"
            else -> return
        }

        val uuid = itemStack.getItemUuid() ?: return

        val fakeInventory = compactorMap.getOrPut(uuid) {
            val slots = slotsMap[tier] ?: return
            val itemList = (0 until slots).map { slot ->
                val skyblockId = itemStack.getAttributeString(prefix + slot)
                skyblockId?.let { getInternalNameFromHypixelId(it) }?.getItemStack()
            }

            RenderableInventory.fakeInventory(itemList, MAX_ITEMS_PER_ROW, 1.0)
        }

        val title = Renderable.string(name)

        RenderableTooltips.setTooltipForRender(listOf(title, fakeInventory), spacedTitle = true)
        event.cancel()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        compactorMap.clear()
    }


    private fun shouldShow() = when (config.visibilityMode) {
        PersonalCompactorConfig.VisibilityMode.ALWAYS -> true
        PersonalCompactorConfig.VisibilityMode.KEYBIND -> config.keybind.isKeyHeld()
        PersonalCompactorConfig.VisibilityMode.EXCEPT_KEYBIND -> !config.keybind.isKeyHeld()
        else -> false
    }


    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
