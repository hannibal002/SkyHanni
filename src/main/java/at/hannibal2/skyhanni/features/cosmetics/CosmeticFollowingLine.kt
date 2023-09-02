package at.hannibal2.skyhanni.features.cosmetics

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzUtils.toChromaColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class CosmeticFollowingLine {
    private val config get() = SkyHanniMod.feature.misc.cosmeticConfig.followingLineConfig

    private var locations = mapOf<LorenzVec, LocationSpot>()

    class LocationSpot(val time: SimpleTimeMark, val onGround: Boolean)

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        locations = emptyMap()
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return


        val firstPerson = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0

        val last7 = locations.keys.toList().takeLast(7)
        val last2 = locations.keys.toList().takeLast(2)

        val maxWidth = config.lineWidth
        for ((a, b) in locations.keys.zipWithNext()) {
            val locationSpot = locations[b]!!
            if (firstPerson) {
                if (!locationSpot.onGround) {
                    if (b in last7) {
                        // Do not render the line in the face, keep more distance while the line is in the air
                        continue
                    }
                }
            }
            if (b in last2) {
                // Do not render the line directly next to the player, prevent laggy design
                continue
            }

            val millis = locationSpot.time.passedSince().inWholeMilliseconds
            val percentage = millis.toDouble() / (config.secondsAlive * 1000.0)
            val rawLineWidth = 1 + maxWidth - percentage * maxWidth
            var lineWidth = rawLineWidth.toInt()
            if (lineWidth < 1) {
                lineWidth = 1
            }
            event.draw3DLine(a, b, config.lineColor.toChromaColor(), lineWidth, !config.behindBlocks)
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return

        if (event.isMod(5)) {
            locations = locations.editCopy { values.removeIf { it.time.passedSince() > config.secondsAlive.seconds } }

            // Safety check to not cause lags
            while (locations.size > 5_000) {
                locations = locations.editCopy { remove(keys.first()) }
            }
        }


        if (event.isMod(2)) {
            val playerLocation = LocationUtils.playerLocation().add(0.0, 0.3, 0.0)

            locations.keys.lastOrNull()?.let {
//                if (it.distance(playerLocation) < 0.05) return
                if (it.distance(playerLocation) < 0.1) return
            }

            locations = locations.editCopy {
                val locationSpot = LocationSpot(SimpleTimeMark.now(), Minecraft.getMinecraft().thePlayer.onGround)
//                if (locations.size > 1) {
//                    val last = locations.keys.last()
//                    var distance = last.distance(playerLocation)
//                    if (distance > 65) {
//                        println("skipped distance: $distance")
//                        clear()
//                        return@editCopy
//                    }
//
//                    // We split the distance a player moves in 0.2 block long parts
//                    val partLength = 0.2
//                    val direction = playerLocation.subtract(last).normalize().multiply(partLength)
//                    var help = last
//                    var i = 0 // Just for safety
//                    while (distance > partLength) {
//                        distance -= partLength
//                        help = help.add(direction)
//                        this[help] = locationSpot
//
//                        if (i++ == ((1 / partLength) * 65).toInt()) break
//                    }
//                }
                this[playerLocation] = locationSpot
            }
        }
    }
}
