package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class BurrowWarpHelper {

    private var lastWarpTime = SimpleTimeMark.farPast()
    private var lastWarp: WarpPoint? = null

    @SubscribeEvent
    fun onKeyClick(event: LorenzKeyPressEvent) {
        if (!DianaAPI.featuresEnabled()) return
        if (!config.burrowNearestWarp) return

        if (event.keyCode != config.keyBindWarp) return
        if (Minecraft.getMinecraft().currentScreen != null) return

        currentWarp?.let {
            if (lastWarpTime.passedSince() > 5.seconds) {
                lastWarpTime = SimpleTimeMark.now()
                LorenzUtils.sendCommandToServer("warp " + currentWarp?.name)
                lastWarp = currentWarp
            }
        }
    }

    @SubscribeEvent
    fun onStatusBar(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (event.message == "§cYou haven't unlocked this fast travel destination!") {
            if (lastWarpTime.passedSince() < 1.seconds) {
                lastWarp?.let {
                    it.unlocked = false
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

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        lastWarp = null
        currentWarp = null
    }

    companion object {
        private val config get() = SkyHanniMod.feature.event.diana
        var currentWarp: WarpPoint? = null

        fun shouldUseWarps(target: LorenzVec) {
            val playerLocation = LocationUtils.playerLocation()
            val warpPoint = getNearestWarpPoint(target)

            val playerDistance = playerLocation.distance(target)
            val warpDistance = warpPoint.distance(target)
            val difference = playerDistance - warpDistance
            currentWarp = if (difference > 10) {
                warpPoint
            } else {
                null
            }
        }

        private fun getNearestWarpPoint(location: LorenzVec) =
            WarpPoint.entries.filter { it.unlocked && !it.ignored() }.map { it to it.distance(location) }
                .sorted().first().first

        fun resetDisabledWarps() {
            WarpPoint.entries.forEach { it.unlocked = true }
            LorenzUtils.chat("§e[SkyHanni] Reset disabled burrow warps.")
        }
    }

    enum class WarpPoint(
        val displayName: String,
        private val location: LorenzVec,
        private val extraBlocks: Int,
        val ignored: () -> Boolean = { false },
        var unlocked: Boolean = true,
    ) {
        HUB("Hub", LorenzVec(-3, 70, -70), 2),
        CASTLE("Castle", LorenzVec(-250, 130, 45), 10),
        CRYPT("Crypt", LorenzVec(-190, 74, -88), 15, { config.ignoredWarps.crypt }),
        DA("Dark Auction", LorenzVec(91, 74, 173), 2),
        MUSEUM("Museum", LorenzVec(-75, 76, 81), 2),
        WIZARD("Wizard", LorenzVec(42.5, 122.0, 69.0), 5, { config.ignoredWarps.wizard }),
        ;

        fun distance(other: LorenzVec): Double = other.distance(location) + extraBlocks
    }
}
