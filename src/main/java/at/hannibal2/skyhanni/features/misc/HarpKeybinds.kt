package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.InventoryUtils.openInventoryName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import kotlin.time.Duration.Companion.milliseconds

// Delaying key presses by 300ms comes from NotEnoughUpdates
class HarpKeybinds {
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

    @SubscribeEvent
    fun onGui(event: GuiScreenEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.misc.harpKeybinds) return
        if (!openInventoryName().startsWith("Harp")) return
        val chest = event.gui as? GuiChest ?: return

        for (key in keys) {
            if (Keyboard.isKeyDown(key)) {
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
}