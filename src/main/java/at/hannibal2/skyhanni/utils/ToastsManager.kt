package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MinecraftData
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.ToastRenderEvent
import at.hannibal2.skyhanni.utils.NEUItems.renderOnScreen
import net.minecraft.client.Minecraft
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object ToastsManager {
    private val config get() = SkyHanniMod.feature.gui.toasts

    private lateinit var currentToast: Toast
    private var toastActive = false
    private val expiredToasts = mutableListOf<Toast>()

    fun displayToast(text: List<String>, item: ItemStack?) {
        cancelToast(false)
        val duration = config.displayDuration.toDouble().seconds
        currentToast = Toast(text, item, SimpleTimeMark.now(), SimpleTimeMark.now().plus(duration))
        toastActive = true
    }

    private fun cancelToast(immediately: Boolean) {
        if (!toastActive) return
        toastActive = false
        if (!config.animations) return
        if (immediately) return
        currentToast.endTime = SimpleTimeMark.now()
        expiredToasts.add(currentToast)
    }

    @SubscribeEvent
    fun onToastRender(event: ToastRenderEvent) {
        if (toastActive && currentToast.endTime.isInPast()) {
            cancelToast(false)
        }

        val expired = expiredToasts
        val toastsToRemove = mutableListOf<Toast>()

        for (toast in expired) {
            if (toast.endTime.passedSince() >= 350.milliseconds) {
                toastsToRemove.add(toast)
                continue
            }

            renderToast(toast)
        }

        for (toast in toastsToRemove)  {
            expiredToasts.remove(toast)
        }


        if (!toastActive) return

        renderToast(currentToast)
    }

    private fun renderToast(toast: Toast) {
        var offsetX = 0
        var offsetY = 0
        val width = toast.getWidth()
        val height = toast.getHeight()
        val shownFor = toast.startTime.passedSince()
        val expiredFor = toast.endTime.passedSince()
        val expired = expiredFor.isPositive()
        var animationProgress = 0.0


        var status = when {
            expired && expiredFor < 350.milliseconds -> {
                animationProgress = expiredFor.inWholeMilliseconds / 350.0
                ToastStatus.LEAVING
            }
            shownFor < 500.milliseconds -> {
                animationProgress = shownFor.inWholeMilliseconds / 500.0
                ToastStatus.ENTERING
            }
            else -> {
                ToastStatus.DISPLAYING
            }
        }

        if (!config.animations) status = ToastStatus.DISPLAYING

        if (status != ToastStatus.DISPLAYING) {
            if (config.direction == 0) {
                // horizontal transition
                val distance = (toast.getWidth() * (1 - animationProgress)).toInt()
                offsetX = if (config.corner == 0) - distance else distance
            } else {
                // vertical transition
                offsetY = - ((toast.getHeight()) * (1 - animationProgress)).toInt()
            }
        }

        if (status == ToastStatus.LEAVING) {
            if (config.direction == 0) {
                if (config.corner == 0) offsetX = offsetX * -1 - width
                if (config.corner == 1) offsetX = offsetX * -1 + width
            }
            if (config.direction == 1) offsetY = offsetY * -1 - height
        }


        val posX = if (config.corner == 0) 5 else MinecraftData.scaledWidth() - 5 - width
        val posY = 5

        GuiRenderUtils.drawAlphaRectangle(posX + offsetX, posY + offsetY, width, height)

        for ((index, line) in toast.toastLines.withIndex()) {
            var textOffset = 0

            if (toast.itemActive() && config.corner == 0) {
                textOffset = 30
            }

            GuiRenderUtils.drawString(line, posX + 4 + offsetX + textOffset, posY + 5 + offsetY + index * 10)
        }

        if (toast.itemActive()) {
            var itemPosX = posX + 4 + offsetX + 9
            val itemPosY = posY + 5 + offsetY + height / 2 - 8
            if (config.corner == 1) itemPosX += width - 35
            toast.toastItem!!.renderOnScreen(itemPosX, itemPosY, 2.0)
        }
    }

    private fun Toast.getHeight(): Int {
        val height = this.toastLines.size * 10 + 10
        return if (this.itemActive() && height < 30) 30 else height
    }

    private fun Toast.getWidth(): Int {
        var width = 0
        for (line in this.toastLines) {
            val length = Minecraft.getMinecraft().fontRendererObj.getStringWidth(line) + 8
            if (length > width) width = length
        }

        return if (this.itemActive()) width + 30 else width
    }

    private fun Toast.itemActive() = config.items && this.toastItem != null

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        cancelToast(true)
        expiredToasts.clear()
    }

    fun command(args: Array<String>) {
        val list = args.toList()
        displayToast(list, ItemStack(Items.gold_ingot))
    }
}

enum class ToastStatus {
    ENTERING,
    DISPLAYING,
    LEAVING
}