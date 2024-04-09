package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.BitsUpdateEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.createSound
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class NoBitsWarning {

    private val config get() = SkyHanniMod.feature.misc.noBitsWarning

    @SubscribeEvent
    fun onBitsUpdate(event: BitsUpdateEvent) {
        if (!isEnabled()) return
        if (event.bitsToClaim != 0) return

        ChatUtils.clickableChat(
            "§bNo Bits Available! §eClick to run /bz booster cookie.",
            "bz booster cookie"
        )
        LorenzUtils.sendTitle("§bNo Bits Available", 5.seconds)
        SoundUtils.repeatSound(100,10, createSound("note.pling", 0.6f))
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config
}
