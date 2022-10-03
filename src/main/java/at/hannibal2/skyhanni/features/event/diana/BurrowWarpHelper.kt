package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.OSUtils.isActive
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard

class BurrowWarpHelper {

    private val keyBinding = KeyBinding(
        "Nearest Burrow Warp",
        Keyboard.KEY_X,
        "SkyHanni"
    )

    private var lastWarpTime = 0L
    private var lastWarp: WarpPoint? = null

    init {
        ClientRegistry.registerKeyBinding(keyBinding)
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (keyBinding.isActive()) {
            currentWarp?.let {
                if (System.currentTimeMillis() > lastWarpTime + 5_000) {
                    val thePlayer = Minecraft.getMinecraft().thePlayer
                    thePlayer.sendChatMessage("/warp " + currentWarp?.name)
                    lastWarp = currentWarp
                    lastWarpTime = System.currentTimeMillis()
                }
            }
        }
    }

    @SubscribeEvent
    fun onStatusBar(event: LorenzChatEvent) {
        if (!HypixelData.skyblock) return

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
            return map.toList().sortedBy { (_, value) -> value }.first().first
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