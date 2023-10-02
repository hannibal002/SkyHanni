package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
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
import kotlin.time.Duration.Companion.seconds

class GardenOptimalSpeed {
    private val config get() = SkyHanniMod.feature.garden.optimalSpeed
    private val configCustomSpeed get() = config.customSpeed
    private var currentSpeed = 100
    private var optimalSpeed = -1
    private val currentSpeedPattern = " Speed: §r§f✦(?<speed>.*)".toPattern()
    private var lastWarnTime = 0L
    private var cropInHand: CropType? = null
    private var rancherOverlayList: List<List<Any?>> = emptyList()

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
        if (isEnabled()) {
            optimalSpeed = cropInHand.let { it?.getOptimalSpeed() ?: -1 }
        }
    }

    private fun CropType.getOptimalSpeed() = when (this) {
        CropType.WHEAT -> configCustomSpeed.wheat
        CropType.CARROT -> configCustomSpeed.carrot
        CropType.POTATO -> configCustomSpeed.potato
        CropType.NETHER_WART -> configCustomSpeed.netherWart
        CropType.PUMPKIN ->  configCustomSpeed.pumpkin
        CropType.MELON ->  configCustomSpeed.melon
        CropType.COCOA_BEANS -> configCustomSpeed.cocoaBeans
        CropType.SUGAR_CANE -> configCustomSpeed.sugarCane
        CropType.CACTUS -> configCustomSpeed.cactus
        CropType.MUSHROOM -> configCustomSpeed.mushroom
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        if (optimalSpeed == -1) return

        val text = "Optimal Speed: §f$optimalSpeed"
        if (optimalSpeed != currentSpeed) {
            config.pos.renderString("§c$text", posLabel = "Garden Optimal Speed")
             warn()
        } else {
            config.pos.renderString("§a$text", posLabel = "Garden Optimal Speed")
        }
    }

    private fun warn() {
        if (!config.warning) return
        if (!Minecraft.getMinecraft().thePlayer.onGround) return
        if (GardenAPI.onBarnPlot) return
        if (System.currentTimeMillis() < lastWarnTime + 20_000) return

        lastWarnTime = System.currentTimeMillis()
        TitleUtils.sendTitle("§cWrong speed!", 3.seconds)
        cropInHand?.let {
            LorenzUtils.chat("§e[SkyHanni] Wrong speed for ${it.cropName}: §f$currentSpeed §e(§f$optimalSpeed §eis optimal)")
        }
    }

    private fun isRancherOverlayEnabled() = GardenAPI.inGarden() && config.signEnabled
    private fun isEnabled() = GardenAPI.inGarden() && config.enabled

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent){
        event.move(3,"garden.optimalSpeedEnabled", "garden.optimalSpeed.enabled")
        event.move(3,"garden.optimalSpeedWarning", "garden.optimalSpeed.warning")
        event.move(3,"garden.optimalSpeedSignEnabled", "garden.optimalSpeed.signEnabled")
        event.move(3,"garden.optimalSpeedSignPosition", "garden.optimalSpeed.signPosition")
        event.move(3,"garden.optimalSpeedPos", "garden.optimalSpeed.pos")
        event.move(3,"garden.optimalSpeedCustom.wheat", "garden.optimalSpeed.customSpeed.wheat")
        event.move(3,"garden.optimalSpeedCustom.carrot", "garden.optimalSpeed.customSpeed.carrot")
        event.move(3,"garden.optimalSpeedCustom.potato", "garden.optimalSpeed.customSpeed.potato")
        event.move(3,"garden.optimalSpeedCustom.netherWart", "garden.optimalSpeed.customSpeed.netherWart")
        event.move(3,"garden.optimalSpeedCustom.pumpkin", "garden.optimalSpeed.customSpeed.pumpkin")
        event.move(3,"garden.optimalSpeedCustom.melon", "garden.optimalSpeed.customSpeed.melon")
        event.move(3,"garden.optimalSpeedCustom.cocoaBeans", "garden.optimalSpeed.customSpeed.cocoaBeans")
        event.move(3,"garden.optimalSpeedCustom.sugarCane", "garden.optimalSpeed.customSpeed.sugarCane")
        event.move(3,"garden.optimalSpeedCustom.cactus", "garden.optimalSpeed.customSpeed.cactus")
        event.move(3,"garden.optimalSpeedCustom.mushroom", "garden.optimalSpeed.customSpeed.mushroom")
    }
}