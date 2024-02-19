package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.BurrowDetectEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object AllBurrowsList {
    private var list = listOf<LorenzVec>()
    private val config get() = SkyHanniMod.feature.event.diana.allBurrowsList
    private val storage get() = ProfileStorageData.profileSpecific?.diana

    @SubscribeEvent
    fun onBurrowDetect(event: BurrowDetectEvent) {
        if (!isEnabled()) return
        val storage = storage ?: return
        storage.foundBurrowLocations.add(event.burrowLocation)
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.repeatSeconds(1)) return
        val storage = storage ?: return

        val range = 5..70
        list = storage.foundBurrowLocations.asSequence().map { it to it.distanceToPlayer() }
            .filter { it.second.toInt() in range }
            .sortedBy { it.second }
            .map { it.first }
            .take(25).toList()
    }

    fun copyToClipboard() {
        val storage = storage ?: return
        val list = storage.foundBurrowLocations.map { it.printWithAccuracy(0, ":") }
        OSUtils.copyToClipboard(list.joinToString(";"))
        LorenzUtils.chat("Saved all ${list.size} burrow locations to clipboard.")
    }

    fun addFromClipboard() {
        SkyHanniMod.coroutineScope.launch {
            val text = OSUtils.readFromClipboard() ?: return@launch
            val storage = storage ?: return@launch

            val list = storage.foundBurrowLocations
            var new = 0
            var duplicate = 0
            for (raw in text.split(";")) {
                val location = LorenzVec.decodeFromString(raw)
                if (location !in list) {
                    list.add(location)
                    new++
                } else {
                    duplicate++
                }
            }
            ChatUtils.chat("Added $new new burrow locations, $duplicate are duplicate.")
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (!config.showAll) return

        for (location in list) {
            event.drawColor(location, LorenzColor.DARK_AQUA)
        }
    }

    fun isEnabled() = DianaAPI.isDoingDiana() && config.save
}
