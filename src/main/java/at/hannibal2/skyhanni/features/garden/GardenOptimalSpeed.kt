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
    private var currentSpeed = 100
    private var optimalSpeed = -1
    private val currentSpeedPattern = Pattern.compile(" Speed: §r§f✦(.*)")
    private var lastWarnTime = 0L
    private var cropInHand: String? = null

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
        cropInHand = if (event.isRealCrop) event.crop else null
        if (isEnabled()) {
            optimalSpeed = cropInHand.let { if (it != null) speedForCrop(it) else -1 }
        }
    }

    private fun speedForCrop(crop: String) = when (crop) {
        "Wheat" -> 93
        "Carrot" -> 93
        "Potato" -> 93
        "Pumpkin" -> 155
        "Sugar Cane" -> 328
        "Melon" -> 155
        "Cactus" -> 400 // 500 with racing helmet
        "Cocoa Beans" -> 155
        "Mushroom" -> 233
        "Nether Wart" -> 93

        else -> -1
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!Minecraft.getMinecraft().thePlayer.onGround) return

        if (optimalSpeed == -1) return

        val text = "Optimal Speed: §f$optimalSpeed"
        if (optimalSpeed != currentSpeed) {
            config.optimalSpeedPos.renderString("§c$text")
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
            config.optimalSpeedPos.renderString("§a$text")
        }
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.optimalSpeedEnabled
}