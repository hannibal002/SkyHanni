package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard

class PasteIntoSigns {

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!HypixelData.hypixel) return
        if (!SkyHanniMod.feature.misc.pasteIntoSigns) return

        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_V)) {
            val clipboard = OSUtils.readFromClipboard() ?: return
            LorenzUtils.setTextIntoSign(clipboard.take(15))
        }
    }
}