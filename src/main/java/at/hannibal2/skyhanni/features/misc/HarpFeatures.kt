package at.hannibal2.skyhanni.features.misc

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
import org.lwjgl.input.Keyboard
import kotlin.time.Duration.Companion.milliseconds

// Delaying key presses by 300ms comes from NotEnoughUpdates
class HarpFeatures {
    private val config get() = SkyHanniMod.feature.inventory.helper.harp
    private var lastClick = SimpleTimeMark.farPast()

    private val keys = listOf(
        Keyboard.KEY_1,
        Keyboard.KEY_2,
        Keyboard.KEY_3,
        Keyboard.KEY_4,
        Keyboard.KEY_5,
        Keyboard.KEY_6,
        Keyboard.KEY_7
    )

    private val buttonColors = listOf('d', 'e', 'a', '2', '5', '9', 'b')

    @SubscribeEvent
    fun onGui(event: GuiScreenEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.keybinds) return
        if (!openInventoryName().startsWith("Harp")) return
        val chest = event.gui as? GuiChest ?: return

        for (key in keys) {
            if (key.isKeyHeld()) {
                if (lastClick.passedSince() > 200.milliseconds) {
                    Minecraft.getMinecraft().playerController.windowClick(
                        chest.inventorySlots.windowId,
                        35 + key,
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