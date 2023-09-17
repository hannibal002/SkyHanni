package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PasteIntoSigns {
    private var lastClicked = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.onHypixel) return
        if (!SkyHanniMod.feature.misc.pasteIntoSigns) return

        val currentlyClicked = LorenzUtils.isPastingKeysDown()
        if (!lastClicked && currentlyClicked) {
            SkyHanniMod.coroutineScope.launch {
                OSUtils.readFromClipboard()?.let {
                    LorenzUtils.addTextIntoSign(it.take(15))
                }
            }
        }
        lastClicked = currentlyClicked
    }
}