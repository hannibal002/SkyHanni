package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ThunderBottleNotification {

    private val config get() = SkyHanniMod.feature.misc

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (!LorenzUtils.inSkyBlock) return

        if (event.message.endsWith("§e> Your bottle of thunder has fully charged!")) {
            LorenzUtils.sendTitle("§eThunder Bottle Charged!", 5.seconds)
        }
    }

    fun isEnabled() = config.thunderBottleNotification
}
