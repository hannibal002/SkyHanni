package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.minecraft.KeyPressEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.GuiRenderUtils
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.compat.GuiScreenUtils
import net.minecraft.client.Minecraft
import org.lwjgl.input.Keyboard
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@HanniModule
object NotificationManager {

    private val notificationQueue = mutableListOf<HanniNotification>()

    private var currentNotification: HanniNotification? = null
    private var lastNotificationClosed = SimpleTimeMark.farPast()

    private const val CLOSE_TEXT = "§c[X] Close"

    @HandleEvent
    fun onKeyPress(event: KeyPressEvent) {
        currentNotification ?: return
        if (lastNotificationClosed.passedSince() < 200.milliseconds) return
        if (event.keyCode != Keyboard.KEY_X) return
        currentNotification = null
        lastNotificationClosed = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        val notification = getCurrentNotification() ?: return

        if (InventoryUtils.inInventory() && !notification.showOverInventory) return

        val midX = GuiScreenUtils.scaledWindowWidth / 2
        val topY = (GuiScreenUtils.scaledWindowHeight * 0.75 - notification.height / 2).toInt()

        GuiRenderUtils.drawFloatingRectDark(midX - notification.width / 2, topY, notification.width, notification.height)
        val closeTextWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(CLOSE_TEXT)

        GuiRenderUtils.drawString(CLOSE_TEXT, midX + notification.width / 2 - 3 - closeTextWidth, topY + 4)

        if (notification.length.isFinite()) {
            val remainingTime = "§8" + notification.endTime.timeUntil().format()
            GuiRenderUtils.drawString(remainingTime, midX - notification.width / 2 + 4, topY + 4)
        }

        notification.message.forEachIndexed { index, line ->
            GuiRenderUtils.drawStringCentered("§7$line", midX, topY + 19 + index * 10)
        }
    }

    private fun getCurrentNotification(): HanniNotification? {
        currentNotification?.let {
            if (it.endTime.isInPast()) currentNotification = null
        }
        if (currentNotification == null) {
            currentNotification = notificationQueue.removeFirstOrNull()
            currentNotification?.setEndTime()
        }
        return currentNotification
    }

    fun queueNotification(notification: HanniNotification) {
        notificationQueue.add(notification)
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestnotification") {
            description = "Shows a test notification"
            category = CommandCategory.DEVELOPER_TEST
            arg("notification", BrigadierArguments.greedyString()) {
                callback {
                    val testingText = getArg(it).replace("\\n", "\n")
                    queueNotification(HanniNotification(testingText, Duration.INFINITE))
                }
            }
        }
    }
}

data class HanniNotification(
    val message: List<String>,
    val length: Duration,
    val showOverInventory: Boolean = false,
) {
    constructor(message: String, length: Duration, showOverInventory: Boolean = false) : this(
        message.lines(),
        length,
        showOverInventory,
    )

    var endTime = SimpleTimeMark.farFuture()

    val width by lazy { (message.maxOfOrNull { Minecraft.getMinecraft().fontRendererObj.getStringWidth(it) } ?: 0) + 8 }
    val height = message.size * 10 + 18

    fun setEndTime() {
        if (length.isInfinite()) return
        endTime = SimpleTimeMark.now() + length
    }
}
