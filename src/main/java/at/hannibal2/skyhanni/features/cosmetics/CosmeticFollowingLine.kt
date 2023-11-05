package at.hannibal2.skyhanni.features.cosmetics

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.enums.OutsideSbFeature
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzUtils.toChromaColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.exactLocation
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CosmeticFollowingLine {
    private val config get() = SkyHanniMod.feature.misc.cosmeticConfig.followingLineConfig

    private var locations = mapOf<LorenzVec, LocationSpot>()
    private var latestLocations = mapOf<LorenzVec, LocationSpot>()

    class LocationSpot(val time: SimpleTimeMark, val onGround: Boolean)

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        locations = emptyMap()
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return

        updateClose(event)

        val firstPerson = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0
        val color = config.lineColor.toChromaColor()

        renderClose(event, firstPerson, color)
        renderFar(event, firstPerson, color)
    }

    private fun renderFar(
        event: LorenzRenderWorldEvent,
        firstPerson: Boolean,
        color: Color
    ) {
        val last7 = locations.keys.toList().takeLast(7)
        val last2 = locations.keys.toList().takeLast(2)

        for ((a, b) in locations.keys.zipWithNext()) {
            val locationSpot = locations[b]!!
            if (firstPerson && !locationSpot.onGround && b in last7) {
                // Do not render the line in the face, keep more distance while the line is in the air
                continue
            }
            if (b in last2 && locationSpot.time.passedSince() < 400.milliseconds) {
                // Do not render the line directly next to the player, prevent laggy design
                continue
            }
            event.draw3DLine(a, b, color, locationSpot.getWidth(), !config.behindBlocks)
        }
    }

    private fun updateClose(event: LorenzRenderWorldEvent) {
        val playerLocation = event.exactLocation(Minecraft.getMinecraft().thePlayer).add(0.0, 0.3, 0.0)

        latestLocations = latestLocations.editCopy {
            val locationSpot = LocationSpot(SimpleTimeMark.now(), Minecraft.getMinecraft().thePlayer.onGround)
            this[playerLocation] = locationSpot
            values.removeIf { it.time.passedSince() > 600.milliseconds }
        }
    }

    private fun renderClose(event: LorenzRenderWorldEvent, firstPerson: Boolean, color: Color) {
        if (firstPerson && latestLocations.any { !it.value.onGround }) return

        for ((a, b) in latestLocations.keys.zipWithNext()) {
            val locationSpot = latestLocations[b]!!
            event.draw3DLine(a, b, color, locationSpot.getWidth(), !config.behindBlocks)
        }
    }

    private fun LocationSpot.getWidth(): Int {
        val millis = time.passedSince().inWholeMilliseconds
        val percentage = millis.toDouble() / (config.secondsAlive * 1000.0)
        val maxWidth = config.lineWidth
        val lineWidth = 1 + maxWidth - percentage * maxWidth
        return lineWidth.toInt().coerceAtLeast(1)
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

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
                if (it.distance(playerLocation) < 0.1) return
            }

            locations = locations.editCopy {
                this[playerLocation] = LocationSpot(SimpleTimeMark.now(), Minecraft.getMinecraft().thePlayer.onGround)
            }
        }
    }

    private fun isEnabled() = (LorenzUtils.inSkyBlock || OutsideSbFeature.FOLLOWING_LINE.isSelected()) && config.enabled
}
