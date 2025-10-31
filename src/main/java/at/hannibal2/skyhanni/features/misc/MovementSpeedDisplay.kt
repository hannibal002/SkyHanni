package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.enums.OutsideSBFeature
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.BlockUtils.getBlockAt
import at.hannibal2.hanni.utils.LocationUtils
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.NumberUtil.roundTo
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.compat.MinecraftCompat
import at.hannibal2.hanni.utils.system.PlatformUtils
import net.minecraft.init.Blocks
import kotlin.concurrent.fixedRateTimer

@HanniModule
object MovementSpeedDisplay {

    private val config get() = HanniMod.feature.misc

    private var display = ""
    private val soulSandSpeeds = mutableListOf<Double>()

    /**
     * This speed value represents the movement speed in blocks per second.
     * This has nothing to do with the speed stat.
     */
    var speed = 0.0
    var usingLegacySoulSandSpeed = false

    init {
        // TODO use LorenzTickEvent
        fixedRateTimer(name = "hanni-movement-speed-display", period = 250, initialDelay = 1_000) {
            checkSpeed()
        }
    }

    private fun checkSpeed() {
        if (!SkyBlockUtils.onHypixel) return

        speed = with(MinecraftCompat.localPlayer) {
            val oldPos = LorenzVec(prevPosX, prevPosY, prevPosZ)
            val newPos = LorenzVec(posX, posY, posZ)

            // Distance from previous tick, multiplied by TPS
            oldPos.distance(newPos) * 20
        }

        // 1.15+ has consistent soul sand speed
        val movingOnSoulSand = PlatformUtils.IS_LEGACY && LocationUtils.playerLocation().getBlockAt() == Blocks.soul_sand && speed > 0.0
        if (movingOnSoulSand) {
            soulSandSpeeds.add(speed)
            if (soulSandSpeeds.size > 6) {
                speed = soulSandSpeeds.average()
                soulSandSpeeds.removeAt(0)
            }
        } else {
            soulSandSpeeds.clear()
        }
        usingLegacySoulSandSpeed = movingOnSoulSand && soulSandSpeeds.size == 6

        if (isEnabled()) {
            display = "Movement Speed: ${speed.roundTo(2)}"
        }
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.playerMovementSpeedPos.renderString(display, posLabel = "Movement Speed")
    }

    fun isEnabled() = SkyBlockUtils.onHypixel &&
        (SkyBlockUtils.inSkyBlock || OutsideSBFeature.MOVEMENT_SPEED.isSelected()) &&
        config.playerMovementSpeed
}
