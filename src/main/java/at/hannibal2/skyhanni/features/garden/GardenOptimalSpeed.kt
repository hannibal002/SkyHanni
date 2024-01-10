package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isRancherSign
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class GardenOptimalSpeed {
    private val config get() = GardenAPI.config.optimalSpeeds
    private val configCustomSpeed get() = config.customSpeed
    private var sneakingTime = 0.seconds
    private val sneaking get() = Minecraft.getMinecraft().thePlayer.isSneaking
    private val sneakingPersistent get() = sneakingTime > 5.seconds
    private var _currentSpeed = 100
    private var currentSpeed: Int
        get() = (_currentSpeed * (if (sneaking) 0.3 else 1.0)).toInt()
        set(value) {
            _currentSpeed = value
        }
    private var optimalSpeed = -1
    private val currentSpeedPattern = " Speed: §r§f✦(?<speed>.*)".toPattern()
    private var lastWarnTime = 0L
    private var cropInHand: CropType? = null
    private var rancherOverlayList: List<List<Any?>> = emptyList()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (sneaking) {
            sneakingTime += 50.milliseconds
        } else {
            sneakingTime = 0.seconds
        }
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        for (line in event.tabList) {
            currentSpeedPattern.matchMatcher(line) {
                currentSpeed = group("speed").toInt()
            }
        }
    }

    @SubscribeEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        rancherOverlayList = CropType.entries.map { crop ->
            listOf(crop.icon, Renderable.link("${crop.cropName} - ${crop.getOptimalSpeed()}") {
                LorenzUtils.setTextIntoSign("${crop.getOptimalSpeed()}")
            })
        }
    }

    @SubscribeEvent
    fun onGuiRender(event: DrawScreenEvent.Post) {
        if (!isRancherOverlayEnabled()) return
        val gui = event.gui
        if (gui !is GuiEditSign) return
        if (!gui.isRancherSign()) return
        config.signPosition.renderStringsAndItems(
            rancherOverlayList,
            posLabel = "Optimal Speed Rancher Overlay"
        )
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        cropInHand = event.crop
        optimalSpeed = cropInHand.let { it?.getOptimalSpeed() ?: -1 }
    }

    private fun CropType.getOptimalSpeed() = when (this) {
        CropType.WHEAT -> configCustomSpeed.wheat
        CropType.CARROT -> configCustomSpeed.carrot
        CropType.POTATO -> configCustomSpeed.potato
        CropType.NETHER_WART -> configCustomSpeed.netherWart
        CropType.PUMPKIN -> configCustomSpeed.pumpkin
        CropType.MELON -> configCustomSpeed.melon
        CropType.COCOA_BEANS -> configCustomSpeed.cocoaBeans
        CropType.SUGAR_CANE -> configCustomSpeed.sugarCane
        CropType.CACTUS -> configCustomSpeed.cactus
        CropType.MUSHROOM -> configCustomSpeed.mushroom
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!GardenAPI.inGarden()) return

        if (optimalSpeed == -1) return

        if (GardenAPI.hideExtraGuis()) return

        var text = "Optimal Speed: §f$optimalSpeed"
        if (optimalSpeed != currentSpeed) {
            text += " (§eCurrent: §f$currentSpeed"
            if (sneaking) text += " §7[Sneaking]"
            text += "§f)"

            if (config.showOnHUD) config.pos.renderString("§c$text", posLabel = "Garden Optimal Speed")
            if (sneaking && !sneakingPersistent) return
            warn()
        } else {
            if (config.showOnHUD) config.pos.renderString("§a$text", posLabel = "Garden Optimal Speed")
        }
    }

    private fun warn() {
        if (!config.warning) return
        if (!Minecraft.getMinecraft().thePlayer.onGround) return
        if (GardenAPI.onBarnPlot) return
        if (System.currentTimeMillis() < lastWarnTime + 20_000) return

        lastWarnTime = System.currentTimeMillis()
        LorenzUtils.sendTitle("§cWrong speed!", 3.seconds)
        cropInHand?.let {
            var text = "Wrong speed for ${it.cropName}: §f$currentSpeed"
            if (sneaking) text += " §7[Sneaking]"
            text += " §e(§f$optimalSpeed §eis optimal)"

            LorenzUtils.chat(text)
        }
    }

    private fun isRancherOverlayEnabled() = GardenAPI.inGarden() && config.signEnabled

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.optimalSpeedEnabled", "garden.optimalSpeeds.enabled")
        event.move(3, "garden.optimalSpeedWarning", "garden.optimalSpeeds.warning")
        event.move(3, "garden.optimalSpeedSignEnabled", "garden.optimalSpeeds.signEnabled")
        event.move(3, "garden.optimalSpeedSignPosition", "garden.optimalSpeeds.signPosition")
        event.move(3, "garden.optimalSpeedPos", "garden.optimalSpeeds.pos")
        event.move(3, "garden.optimalSpeedCustom.wheat", "garden.optimalSpeeds.customSpeed.wheat")
        event.move(3, "garden.optimalSpeedCustom.carrot", "garden.optimalSpeeds.customSpeed.carrot")
        event.move(3, "garden.optimalSpeedCustom.potato", "garden.optimalSpeeds.customSpeed.potato")
        event.move(3, "garden.optimalSpeedCustom.netherWart", "garden.optimalSpeeds.customSpeed.netherWart")
        event.move(3, "garden.optimalSpeedCustom.pumpkin", "garden.optimalSpeeds.customSpeed.pumpkin")
        event.move(3, "garden.optimalSpeedCustom.melon", "garden.optimalSpeeds.customSpeed.melon")
        event.move(3, "garden.optimalSpeedCustom.cocoaBeans", "garden.optimalSpeeds.customSpeed.cocoaBeans")
        event.move(3, "garden.optimalSpeedCustom.sugarCane", "garden.optimalSpeeds.customSpeed.sugarCane")
        event.move(3, "garden.optimalSpeedCustom.cactus", "garden.optimalSpeeds.customSpeed.cactus")
        event.move(3, "garden.optimalSpeedCustom.mushroom", "garden.optimalSpeeds.customSpeed.mushroom")

        event.move(14, "garden.optimalSpeeds.enabled", "garden.optimalSpeeds.showOnHUD")
    }
}
