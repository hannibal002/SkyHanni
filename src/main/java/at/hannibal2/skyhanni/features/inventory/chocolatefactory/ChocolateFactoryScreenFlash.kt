package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.config.features.inventory.chocolatefactory.ChocolateFactoryRabbitWarningConfig.FlashScreenTypeEntry
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI.specialRabbitTextures
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryDataLoader.clickMeGoldenRabbitPattern
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryDataLoader.clickMeRabbitPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColorInt
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.sin

@SkyHanniModule
object ChocolateFactoryScreenFlash {

    private val config get() = ChocolateFactoryAPI.config
    var flashScreen = false

    @SubscribeEvent
    fun onTick(event: SecondPassedEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        flashScreen = InventoryUtils.getItemsInOpenChest().any {
            when (config.rabbitWarning.flashScreenType) {
                FlashScreenTypeEntry.SPECIAL -> {
                    clickMeGoldenRabbitPattern.matches(it.stack.name) ||
                        it.stack.getSkullTexture() in specialRabbitTextures
                }

                FlashScreenTypeEntry.ALL -> {
                    clickMeRabbitPattern.matches(it.stack.name) ||
                        clickMeGoldenRabbitPattern.matches(it.stack.name) ||
                        it.stack.getSkullTexture() in specialRabbitTextures
                }

                FlashScreenTypeEntry.NONE -> false
            }
        }
    }

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        if (!flashScreen) return
        val mc = Minecraft.getMinecraft()
        val alpha = ((2 + sin(System.currentTimeMillis().toDouble() / 1000)) * 255 / 4).toInt().coerceIn(0..255)
        Gui.drawRect(
            0,
            0,
            mc.displayWidth,
            mc.displayHeight,
            (alpha shl 24) or (config.rabbitWarning.flashColor.toChromaColorInt() and 0xFFFFFF),
        )
        GlStateManager.color(1F, 1F, 1F, 1F)
    }
}
