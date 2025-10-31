package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.ItemAddEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.SkyBlockUtils
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import kotlin.time.Duration.Companion.hours

@HanniModule(neuRequired = true)
object FixNeuHeavyPearls {

    private val config get() = HanniMod.feature.misc
    private val heavyPearl = "HEAVY_PEARL".toInternalName()

    @HandleEvent
    fun onItemAdd(event: ItemAddEvent) {
        if (!isEnabled()) return

        if (event.internalName == heavyPearl && event.amount == 3) {
            val specific = NotEnoughUpdates.INSTANCE.config.getProfileSpecific()
            if (System.currentTimeMillis() > specific.dailyHeavyPearlCompleted + 1.hours.inWholeMilliseconds) {
                ChatUtils.chat("Mark NEU Heavy Pearls as done.")
                specific.dailyHeavyPearlCompleted = System.currentTimeMillis()
            }
        }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.fixNeuHeavyPearls
}
