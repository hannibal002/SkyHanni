package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils.openInventoryName
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.item.Item
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

// Delaying key presses by 300ms comes from NotEnoughUpdates
object HarpFeatures {
    val config get() = SkyHanniMod.feature.inventory.helper.harp
    private var lastClick = SimpleTimeMark.farPast()

    private object keys :
        Iterable<Int> {
        override fun iterator(): Iterator<Int> {
            return object : Iterator<Int> {
                private var currentIndex = 0

                override fun hasNext(): Boolean {
                    return currentIndex < 7
                }

                override fun next(): Int {
                    return when (currentIndex++) {
                        0 -> HarpFeatures.config.harpKeybinds.key1
                        1 -> HarpFeatures.config.harpKeybinds.key2
                        2 -> HarpFeatures.config.harpKeybinds.key3
                        3 -> HarpFeatures.config.harpKeybinds.key4
                        4 -> HarpFeatures.config.harpKeybinds.key5
                        5 -> HarpFeatures.config.harpKeybinds.key6
                        6 -> HarpFeatures.config.harpKeybinds.key7
                        else -> throw NoSuchElementException()
                    }
                }
            }
        }

    }

    private val buttonColors = listOf('d', 'e', 'a', '2', '5', '9', 'b')

    @SubscribeEvent
    fun onGui(event: GuiScreenEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.keybinds) return
        if (!openInventoryName().startsWith("Harp")) return
        val chest = event.gui as? GuiChest ?: return

        for ((index, key) in keys.withIndex()) {
            if (key.isKeyHeld()) {
                if (lastClick.passedSince() > 200.milliseconds) {
                    Minecraft.getMinecraft().playerController.windowClick(
                        chest.inventorySlots.windowId,
                        37 + index,
                        2,
                        3,
                        Minecraft.getMinecraft().thePlayer
                    ) // middle clicks > left clicks
                    lastClick = SimpleTimeMark.now()
                }
                break
            }
        }
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.showNumbers) return
        if (!openInventoryName().startsWith("Harp")) return
        if (Item.getIdFromItem(event.stack.item) != 159) return // Stained hardened clay item id = 159

        // Example: §9| §7Click! will select the 9
        val index = buttonColors.indexOfFirst { it == event.stack.displayName[1] }
        if (index == -1) return // this should never happen unless there's an update

        event.stackTip = (index + 1).toString()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.harpKeybinds", "inventory.helper.harp.keybinds")
        event.move(2, "misc.harpNumbers", "inventory.helper.harp.showNumbers")
    }
}
