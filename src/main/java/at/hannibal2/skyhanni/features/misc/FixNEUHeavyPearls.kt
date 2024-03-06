package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.hours

class FixNEUHeavyPearls {

    private val config get() = SkyHanniMod.feature.misc
    private val heavyPearl = "HEAVY_PEARL".asInternalName()

    @SubscribeEvent
    fun onSackChange(event: ItemAddEvent) {
        if (!isEnabled()) return

        if (event.internalName == heavyPearl && event.amount == 3) {
            val specific = NotEnoughUpdates.INSTANCE.config.getProfileSpecific()
            if (System.currentTimeMillis() > specific.dailyHeavyPearlCompleted + 1.hours.inWholeMilliseconds) {
                ChatUtils.chat("Mark NEU Heavy Pearls as done.")
                specific.dailyHeavyPearlCompleted = System.currentTimeMillis()
            }
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.fixNeuHeavyPearls
}
