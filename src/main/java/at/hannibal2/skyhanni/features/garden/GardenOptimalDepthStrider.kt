package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class GardenOptimalDepthStrider {

    private val config get() = SkyHanniMod.feature.garden.optimalDepthStrider
    private val configCustomDepthStrider get() = config.customDepthStrider
    private var optimalDepthStrider = -1
    private var lastWarnTime = 0L
    private var cropInHand: CropType? = null
    private var currentDepthStrider = -1

    private fun checkBoots(stack: ItemStack) {
        currentDepthStrider = stack.getEnchantments()?.get("depth_strider") ?: 0
    }
    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        InventoryUtils.getArmor()[0]?.let { checkBoots(it) }
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        cropInHand = event.crop
        if (isEnabled()) {
            optimalDepthStrider = cropInHand.let { it?.getOptimalDepthStrider() ?: -1 }
        }
    }

    private fun CropType.getOptimalDepthStrider() = when (this) {
        CropType.WHEAT -> configCustomDepthStrider.wheat
        CropType.CARROT -> configCustomDepthStrider.carrot
        CropType.POTATO -> configCustomDepthStrider.potato
        CropType.NETHER_WART -> configCustomDepthStrider.netherWart
        CropType.PUMPKIN -> configCustomDepthStrider.pumpkin
        CropType.MELON -> configCustomDepthStrider.melon
        CropType.COCOA_BEANS -> configCustomDepthStrider.cocoaBeans
        CropType.SUGAR_CANE -> configCustomDepthStrider.sugarCane
        CropType.CACTUS -> configCustomDepthStrider.cactus
        CropType.MUSHROOM -> configCustomDepthStrider.mushroom
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        if (optimalDepthStrider == -1) return

        if (GardenAPI.hideExtraGuis()) return

        val text = "Optimal Depth Strider: §f$optimalDepthStrider"
        if (optimalDepthStrider != currentDepthStrider) {
            config.pos.renderString("§c$text", posLabel = "Garden Optimal Depth Strider")
            warn()
        } else {
            config.pos.renderString("§a$text", posLabel = "Garden Optimal Depth Strider")
        }
    }

    private fun warn() {
        if (!config.warning) return
        if (!Minecraft.getMinecraft().thePlayer.onGround) return
        if (GardenAPI.onBarnPlot) return
        if (System.currentTimeMillis() < lastWarnTime + 20_000) return

        lastWarnTime = System.currentTimeMillis()
        LorenzUtils.sendTitle("§cWrong Depth Strider!", 3.seconds)
        cropInHand?.let {
            LorenzUtils.chat("Wrong depth strider for ${it.cropName}: §f$currentDepthStrider §e(§f$optimalDepthStrider §eis optimal)")
        }
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.enabled && optimalDepthStrider != 0
}

