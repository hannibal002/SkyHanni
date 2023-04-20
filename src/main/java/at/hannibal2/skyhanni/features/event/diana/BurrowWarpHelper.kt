package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent
import org.lwjgl.input.Keyboard

class BurrowWarpHelper {

    private var lastWarpTime = 0L
    private var lastWarp: WarpPoint? = null

    @SubscribeEvent
    fun onKeyBindPressed(event: KeyInputEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.HUB) return
        if (!SkyHanniMod.feature.diana.burrowNearestWarp) return

        if (!Keyboard.getEventKeyState()) return
        val key = if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256 else Keyboard.getEventKey()
        if (SkyHanniMod.feature.diana.keyBindWarp == key) {
            currentWarp?.let {
                if (System.currentTimeMillis() > lastWarpTime + 5_000) {
                    lastWarpTime = System.currentTimeMillis()
                    LorenzUtils.sendCommandToServer("warp " + currentWarp?.name)
                    lastWarp = currentWarp
                }
            }
        }
    }

    @SubscribeEvent
    fun onStatusBar(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (event.message == "§cYou haven't unlocked this fast travel destination!") {
            val time = System.currentTimeMillis() - lastWarpTime
            if (time < 1_000) {
                lastWarp?.let {
                    it.enabled = false
                    LorenzUtils.chat(
                        "§e[SkyHanni] Detected not having access to warp point §b${it.displayName}§e!\n" +
                                "§e[SkyHanni] Use §c/shresetburrowwarps §eonce you have activated this travel scroll."
                    )
                    lastWarp = null
                    currentWarp = null
                }
            }
        }

    }

    companion object {
        var currentWarp: WarpPoint? = null

        fun shouldUseWarps(target: LorenzVec) {
            val playerLocation = LocationUtils.playerLocation()
            val warpPoint = getNearestWarpPoint(target)

            val playerDistance = playerLocation.distance(target)
            val warpDistance = warpPoint.distance(target)
//            println(" ")
//            println("shouldUseWarps")
//            println("playerDistance: ${playerDistance.round(1)}")
//            println("warpDistance: ${warpDistance.round(1)}")
            val difference = playerDistance - warpDistance
            currentWarp = if (difference > 10) {
                warpPoint
            } else {
                null
            }
        }

        private fun getNearestWarpPoint(location: LorenzVec): WarpPoint {
            val map = WarpPoint.values().filter { it.enabled }.map { it to it.distance(location) }
            return map.sorted().first().first
        }

        fun resetDisabledWarps() {
            WarpPoint.values().forEach { it.enabled = true }
            LorenzUtils.chat("§e[SkyHanni] Reset disabled burrow warps.")
        }
    }

    enum class WarpPoint(
        val displayName: String,
        private val location: LorenzVec,
        private val extraBlocks: Int,
        var enabled: Boolean = true,
    ) {
        HUB("Hub", LorenzVec(-3, 70, -70), 2),
        CASTLE("Castle", LorenzVec(-250, 130, 45), 10),

        //        CRYPT("Crypt", LorenzVec(-190, 74, -88), 25),
        DA("Dark Auction", LorenzVec(91, 74, 173), 2),
        MUSEUM("Museum", LorenzVec(-75, 76, 81), 2),
        ;

        fun distance(other: LorenzVec): Double = other.distance(location) + extraBlocks
    }
}