package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

class PasteIntoSigns {

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.onHypixel) return
        if (!SkyHanniMod.feature.misc.pasteIntoSigns) return

        if (LorenzUtils.isControlKeyDown() && Keyboard.isKeyDown(Keyboard.KEY_V)) {
            SkyHanniMod.coroutineScope.launch {
                val clipboard = OSUtils.readFromClipboard() ?: return@launch
                LorenzUtils.setTextIntoSign(clipboard.take(15))
            }
        }
    }
}