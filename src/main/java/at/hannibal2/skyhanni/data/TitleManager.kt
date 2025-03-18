package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import io.github.notenoughupdates.moulconfig.internal.TextRenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import java.util.LinkedList
import java.util.Queue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TitleManager {

    private var currentText = ""
    private var display = ""
    private var endTime = SimpleTimeMark.farPast()
    private var heightModifier = 1.8
    private var fontSizeModifier = 4f

    private val titleQueue: Queue<DisplayTitleData> = LinkedList()
    private val sortTitleQueue: Queue<Map<DisplayTitleData, Double>> = LinkedList()

    data class DisplayTitleData(
        val text: String,
        val duration: Duration,
        val height: Double = 1.8,
        val fontSize: Float = 4f
    )

    fun queueSortTitle(text: String, duration: Duration, height: Double = 1.8, fontSize: Float = 4f, value: Double) {
        sortTitleQueue.add(mapOf(DisplayTitleData(text, duration, height, fontSize) to value))
    }

    fun queueTitle(text: String, duration: Duration, height: Double = 1.8, fontSize: Float = 4f) {
        titleQueue.add(DisplayTitleData(text, duration, height, fontSize))
    }

    fun sendTitle(text: String, duration: Duration, height: Double = 1.8, fontSize: Float = 4f) {
        currentText = text
        display = "§f$text"
        endTime = SimpleTimeMark.now() + duration
        heightModifier = height
        fontSizeModifier = fontSize
    }

    fun optionalResetTitle(condition: (String) -> Boolean) {
        if (condition(currentText)) {
            stop()
        }
    }

    private fun command(args: Array<String>) {
        if (args.size < 4) {
            ChatUtils.userError("Usage: /shsendtitle <duration> <height> <fontSize> <text ..>")
            return
        }

        val duration = args[0].toInt().seconds
        val height = args[1].toDouble()
        val fontSize = args[2].toFloat()
        val title = "§6" + args.drop(3).joinToString(" ").replace("&", "§")

        sendTitle(title, duration, height, fontSize)
    }

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        stop()
    }

    private fun stop() {
        endTime = SimpleTimeMark.farPast()
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (endTime.isInPast()) return

        val width = GuiScreenUtils.scaledWindowWidth
        val height = GuiScreenUtils.scaledWindowHeight

        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        val renderer = Minecraft.getMinecraft().fontRendererObj

        GlStateManager.pushMatrix()
        GlStateManager.translate((width / 2).toFloat(), (height / heightModifier).toFloat(), 3.0f)
        GlStateManager.scale(fontSizeModifier, fontSizeModifier, fontSizeModifier)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(display, renderer, 0f, 0f, true, 75, 0)
        GlStateManager.popMatrix()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shsendtitle") {
            description = "Display a title on the screen with the specified settings."
            category = CommandCategory.DEVELOPER_TEST
            callback { command(it) }
        }
    }

    private fun showNextTitle() {
        if (titleQueue.isNotEmpty()) {
            val title = titleQueue.poll()
            sendTitle(title.text, title.duration, title.height, title.fontSize)
        }
    }

    private fun sortTitles() {
        if (sortTitleQueue.isNotEmpty()) {
            val sorted = sortTitleQueue.poll().toList().sortedBy { it.second }
            titleQueue.add(sorted.first().first)
        }
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (endTime.isInPast()) {
            sortTitles()
            showNextTitle()
        }
    }
}
