package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.BurrowDetectEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object AllBurrowsList {
    private var list = listOf<LorenzVec>()

    @SubscribeEvent
    fun onBurrowDetect(event: BurrowDetectEvent) {
        if (!isEnabled()) return
        val storage = ProfileStorageData.profileSpecific?.diana ?: return
        storage.foundBurrowLocations.add(event.burrowLocation)
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.repeatSeconds(1)) return
        val storage = ProfileStorageData.profileSpecific?.diana ?: return

//         val range = 10..30
//         val range = 10..70
        val range = 5..70
        list = storage.foundBurrowLocations.asSequence().map { it to it.distanceToPlayer() }
            .filter { it.second.toInt() in range }
            .sortedBy { it.second }
            .map { it.first }
            .take(25).toList()
    }

    fun copyToClipboard() {
        val storage = ProfileStorageData.profileSpecific?.diana ?: return
        val list = mutableListOf<String>()
        for (abc in storage.foundBurrowLocations.toList()) {
            val s = abc.printWithAccuracy(0, ":")
            list.add(s)
        }
        val text = list.joinToString(";")
        OSUtils.copyToClipboard(text)
        LorenzUtils.chat("Saved all ${list.size} burrow locations to clipboard.")
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return

        for (location in list) {
//             event.drawColor(location, LorenzColor.LIGHT_PURPLE)
//             event.drawColor(location, LorenzColor.DARK_GRAY)
            event.drawColor(location, LorenzColor.DARK_AQUA)
        }
    }

    fun isEnabled() = DianaAPI.isDoingDiana()
}
