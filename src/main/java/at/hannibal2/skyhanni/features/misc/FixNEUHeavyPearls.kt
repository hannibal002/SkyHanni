package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FixNEUHeavyPearls {
    private val config get() = SkyHanniMod.feature.misc
    private val heavyPearl = "HEAVY_PEARL".asInternalName()

    @SubscribeEvent
    fun onSackChange(event: SackChangeEvent) {
        if (!isEnabled()) return

        for (change in event.sackChanges) {
            if (change.internalName == heavyPearl && change.delta == 3) {
                LorenzUtils.chat("§e[SkyHanni] Mark NEU Heavy Pearls as done.")
                NotEnoughUpdates.INSTANCE.config.getProfileSpecific().dailyHeavyPearlCompleted =
                    System.currentTimeMillis()
            }
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.fixNeuHeavyPearls

}
