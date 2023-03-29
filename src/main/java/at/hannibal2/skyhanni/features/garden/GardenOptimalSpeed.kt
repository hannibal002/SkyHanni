package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SendTitleHelper
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class GardenOptimalSpeed {
    private val config get() = SkyHanniMod.feature.garden
    private val configCustomSpeed get() = config.optimalSpeedCustom
    private var currentSpeed = 100
    private var optimalSpeed = -1
    private val currentSpeedPattern = Pattern.compile(" Speed: §r§f✦(.*)")
    private var lastWarnTime = 0L
    private var cropInHand: CropType? = null

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        for (line in event.tabList) {
            val matcher = currentSpeedPattern.matcher(line)
            if (matcher.matches()) {
                currentSpeed = matcher.group(1).toInt()
            }
        }
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
        if (!Minecraft.getMinecraft().thePlayer.onGround) return

        if (optimalSpeed == -1) return

        val text = "Optimal Speed: §f$optimalSpeed"
        if (optimalSpeed != currentSpeed) {
            config.optimalSpeedPos.renderString("§c$text", posLabel = "Garden Optimal Speed")
            if (config.optimalSpeedWarning) {
                if (System.currentTimeMillis() > lastWarnTime + 20_000) {
                    lastWarnTime = System.currentTimeMillis()
                    SendTitleHelper.sendTitle("§cWrong speed!", 3_000)
                    cropInHand?.let {
                        LorenzUtils.chat("§e[SkyHanni] Wrong speed for $it: §f$currentSpeed §e(§f$optimalSpeed §eis optimal)")
                    }
                }
            }
        } else {
            config.optimalSpeedPos.renderString("§a$text", posLabel = "Garden Optimal Speed")
        }
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.optimalSpeedEnabled
}