package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.enums.OutsideSbFeature
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.concurrent.fixedRateTimer

class MovementSpeedDisplay {

    private val config get() = SkyHanniMod.feature.misc

    private var display = ""
    private val soulsandSpeeds = mutableListOf<Double>()

    companion object {
        /**
         * This speed value represents the walking speed, not the speed stat.
         *
         * It has an absolute speed cap of 500, and items that normally increase the cap do not apply here:
         * (Black Cat pet, Cactus knife, Racing Helmet or Young Dragon Armor)
         *
         * If this information ever gets abstracted away and made available outside this class,
         * and some features need the actual value of the Speed stat instead,
         * we can always just have two separate variables, like walkSpeed and speedStat.
         * But since this change is confined to Garden-specific code, it's fine the way it is for now.
          */
        var speed = 0.0
        var usingSoulsandSpeed = false
    }

    init {
        fixedRateTimer(name = "skyhanni-movement-speed-display", period = 250, initialDelay = 1_000) {
            checkSpeed()
        }
    }

    private fun checkSpeed() {
        if (!LorenzUtils.onHypixel) return

        speed = with(Minecraft.getMinecraft().thePlayer) {
            val oldPos = LorenzVec(prevPosX, prevPosY, prevPosZ)
            val newPos = LorenzVec(posX, posY, posZ)

            // Distance from previous tick, multiplied by TPS
            oldPos.distance(newPos) * 20
        }
        val movingOnSoulsand = LocationUtils.playerLocation().getBlockAt() == Blocks.soul_sand && speed > 0.0
        if (movingOnSoulsand) {
            soulsandSpeeds.add(speed)
            if (soulsandSpeeds.size > 6) {
                speed = soulsandSpeeds.average()
                soulsandSpeeds.removeAt(0)
            }
        } else {
            soulsandSpeeds.clear()
        }
        usingSoulsandSpeed = movingOnSoulsand && soulsandSpeeds.size == 6
        if (isEnabled()) {
            display = "Movement Speed: ${speed.round(2)}"
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.playerMovementSpeedPos.renderString(display, posLabel = "Movement Speed")
    }

    fun isEnabled() = LorenzUtils.onHypixel &&
        (LorenzUtils.inSkyBlock || OutsideSbFeature.MOVEMENT_SPEED.isSelected()) &&
        config.playerMovementSpeed
}
