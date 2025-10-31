package at.hannibal2.hanni.features.garden

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.config.enums.OutsideSBFeature
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.garden.GardenToolChangeEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.LocationUtils
import at.hannibal2.hanni.utils.NumberUtil.roundTo
import at.hannibal2.hanni.utils.RenderUtils.renderStrings
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.compat.MinecraftCompat
import kotlin.time.Duration.Companion.seconds

@HanniModule
object GardenYawAndPitch {

    private val config get() = GardenApi.config.yawPitchDisplay
    private var lastChange = SimpleTimeMark.farPast()
    private var lastYaw = 0f
    private var lastPitch = 0f

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!SkyBlockUtils.onHypixel) return
        if (!isEnabled()) return
        if (GardenApi.hideExtraGuis()) return
        if (GardenApi.toolInHand == null && !config.showWithoutTool) return

        val player = MinecraftCompat.localPlayer
        val yaw = LocationUtils.calculatePlayerYaw()
        val pitch = player.rotationPitch

        if (yaw != lastYaw || pitch != lastPitch) {
            lastChange = SimpleTimeMark.now()
        }
        lastYaw = yaw
        lastPitch = pitch

        if (!config.showAlways && lastChange.passedSince() > config.timeout.seconds) return

        val yawText = yaw.roundTo(config.yawPrecision).toBigDecimal().toPlainString()
        val pitchText = pitch.roundTo(config.pitchPrecision).toBigDecimal().toPlainString()
        val displayList = listOf(
            "§aYaw: §f$yawText",
            "§aPitch: §f$pitchText",
        )
        if (GardenApi.inGarden()) {
            config.pos.renderStrings(displayList, posLabel = "Yaw and Pitch")
        } else {
            config.posOutside.renderStrings(displayList, posLabel = "Yaw and Pitch")
        }
    }

    @HandleEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        lastChange = SimpleTimeMark.farPast()
    }

    private fun isEnabled() =
        config.enabled && (
            (OutsideSBFeature.YAW_AND_PITCH.isSelected() && !SkyBlockUtils.inSkyBlock) ||
                (SkyBlockUtils.inSkyBlock && (GardenApi.inGarden() || config.showOutsideGarden))
            )

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(18, "garden.yawPitchDisplay.showEverywhere", "garden.yawPitchDisplay.showOutsideGarden")
    }
}
