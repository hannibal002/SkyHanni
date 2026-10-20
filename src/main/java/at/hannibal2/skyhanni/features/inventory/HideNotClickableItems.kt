package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.item.ItemNotClickableEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getLowerItems
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawBorder
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.ChestMenu
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HideNotClickableItems {
    private val config get() = SkyHanniMod.feature.inventory.hideNotClickable

    var hideReasons = listOf<String>()
    var showGreenLine = false
    var allowBypass = true

    private var lastClickTime = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnSkyblock = true)
    private fun onForegroundDrawn(event: GuiContainerEvent.ForegroundDrawnEvent) {
        if (bypassActive()) return
        if (event.gui !is ContainerScreen) return
        val chest = event.container as ChestMenu
        val chestName = InventoryUtils.openInventoryName()

        for ((slot, stack) in chest.getLowerItems()) {
            if (hide(chestName, stack)) {
                slot.highlight(LorenzColor.DARK_GRAY.addOpacity(config.transparency))
            } else if (showGreenLine && config.itemsGreenLine) {
                slot.drawBorder(LorenzColor.GREEN.addOpacity(200))
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true, priority = HandleEvent.LOWEST)
    private fun onTooltip(event: ToolTipTextEvent) {
        if (bypassActive()) return

        val guiChest = MinecraftCompat.screen
        if (guiChest !is ContainerScreen) return
        val chestName = InventoryUtils.openInventoryName()

        val stack = event.itemStack
        if (InventoryUtils.getItemsInOpenChest().map { it.item }.contains(stack)) return
        if (!ItemUtils.getItemsInInventory().contains(stack)) return

        if (hide(chestName, stack)) {
            val first = event.toolTip[0]
            event.toolTip.clear()
            event.toolTip.add("§7" + first.string)
            event.toolTip.add("")
            if (hideReasons.isEmpty()) {
                event.toolTip.add("§4No hide reason!")
                ErrorManager.skyHanniError("No hide reason for not clickable item!")
            } else {
                for (string in hideReasons) {
                    event.toolTip.add("§c$string")
                }
                if (config.itemsBypass && allowBypass) {
                    event.toolTip.add("  §7(Bypass by holding the ${KeyboardManager.getModifierKeyName()} key)")
                }
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config.itemsBlockClicks) return
        if (bypassActive()) return
        if (event.gui !is ContainerScreen) return
        val chestName = InventoryUtils.openInventoryName()

        val slot = event.slot ?: return

        if (slot.index == slot.containerSlot) return
        val stack = slot.item.orNull() ?: return

        if (hide(chestName, stack)) {
            event.cancel()

            if (lastClickTime.passedSince() > 5.seconds) {
                lastClickTime = SimpleTimeMark.now()
            }
            return
        }
    }

    private fun bypassActive() = config.itemsBypass && KeyboardManager.isModifierKeyDown() && allowBypass

    private fun hide(chestName: String, stack: SafeItemStack): Boolean {
        val event = ItemNotClickableEvent(chestName, stack)
        event.post()
        hideReasons = event.hideReasons
        showGreenLine = event.showGreenLine
        allowBypass = event.allowBypass
        return hideReasons.isNotEmpty()
    }
}
