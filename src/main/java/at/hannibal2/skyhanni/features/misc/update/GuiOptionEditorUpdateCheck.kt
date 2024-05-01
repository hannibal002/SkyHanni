package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.other.ChangelogJson
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.CollectionUtils.getOrNull
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.fromJson
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import io.github.moulberry.notenoughupdates.itemeditor.GuiElementButton
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.internal.TextRenderUtils
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.EnumChatFormatting.GREEN
import net.minecraft.util.EnumChatFormatting.RED
import org.lwjgl.input.Mouse

class GuiOptionEditorUpdateCheck(option: ProcessedOption) : GuiOptionEditor(option) {

    val button = GuiElementButton("", -1) { }
    val changelog = GuiElementButton("Show Changelog", -1) { }

    val currentVersion = SkyHanniMod.version

    override fun render(x: Int, y: Int, width: Int) {
        val fr = Minecraft.getMinecraft().fontRendererObj

        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat() + 10, y.toFloat(), 1F)
        val width = width - 20
        val nextVersion = UpdateManager.getNextVersion()

        button.text = when (UpdateManager.updateState) {
            UpdateManager.UpdateState.AVAILABLE -> "Download update"
            UpdateManager.UpdateState.QUEUED -> "Downloading..."
            UpdateManager.UpdateState.DOWNLOADED -> "Downloaded"
            UpdateManager.UpdateState.NONE -> if (nextVersion == null) "Check for Updates" else "Up to date"
        }
        button.render(getButtonPosition(width), 10)

        if (UpdateManager.updateState != UpdateManager.UpdateState.NONE) {
            changelog.render(getChangelogPosition(width), 30)
        }

        if (UpdateManager.updateState == UpdateManager.UpdateState.DOWNLOADED) {
            TextRenderUtils.drawStringCentered(
                "${GREEN}The update will be installed after your next restart.", fr, width / 2F, 50F, true, -1
            )
        }

        val widthRemaining = width - button.width - 10

        GlStateManager.scale(2F, 2F, 1F)
        val sameVersion = currentVersion.equals(nextVersion, true)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(
            "${if (UpdateManager.updateState == UpdateManager.UpdateState.NONE) GREEN else RED}$currentVersion" + if (nextVersion != null && !sameVersion) "➜ ${GREEN}${nextVersion}" else "",
            fr,
            widthRemaining / 4F,
            10F,
            true,
            widthRemaining / 2,
            -1
        )

        GlStateManager.popMatrix()
    }

    private fun getButtonPosition(width: Int) = width - button.width
    private fun getChangelogPosition(width: Int) = width - changelog.width
    override fun getHeight(): Int {
        return 55
    }

    private var dispatcher = Dispatchers.IO

    override fun mouseInput(x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int): Boolean {
        val width = width - 20
        if (Mouse.getEventButtonState() && (mouseX - getButtonPosition(width) - x) in (0..button.width) && (mouseY - 10 - y) in (0..button.height)) {
            when (UpdateManager.updateState) {
                UpdateManager.UpdateState.AVAILABLE -> UpdateManager.queueUpdate()
                UpdateManager.UpdateState.QUEUED -> {}
                UpdateManager.UpdateState.DOWNLOADED -> {}
                UpdateManager.UpdateState.NONE -> UpdateManager.checkUpdate()
            }
            return true
        }
        if (Mouse.getEventButtonState() && (mouseX - getChangelogPosition(width) - x) in (0..changelog.width) && (mouseY - 30 - y) in (0..changelog.height)) {
            if (UpdateManager.updateState != UpdateManager.UpdateState.NONE) {
                LorenzDebug.log("Clicked")
                getChangelog()
                openChangelog()
            }
            return true
        }
        return false
    }

    private fun openChangelog() {
        SkyHanniMod.screenToOpen = object : GuiScreen() {
            private val changelogScroll = ScrollValue()

            private var scrollList: Renderable? = null
            private var lastWidth: Int = 0

            override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
                super.drawScreen(mouseX, mouseY, partialTicks)
                val width = 4 * this.width / 5
                val height = 4 * this.height / 5
                val xTranslate = this.width / 10
                val yTranslate = this.height / 10
                RenderUtils.drawRoundGradientRect(
                    xTranslate - 2,
                    yTranslate - 2,
                    width + 4,
                    height + 4,
                    LorenzColor.DARK_GRAY.toColor().darker().withAlpha(220),
                    LorenzColor.DARK_GRAY.toColor().withAlpha(218)
                )
                GlStateManager.translate(xTranslate.toFloat(), yTranslate.toFloat(), 0f)
                Renderable.withMousePosition(mouseX + xTranslate, mouseY + yTranslate) {
                    if (changelogList.isEmpty()) {
                        Renderable.string(
                            "§aStill Loading",
                            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                            verticalAlign = RenderUtils.VerticalAlignment.CENTER
                        )
                    } else {
                        if (lastWidth != width) {
                            scrollList = null
                            lastWidth = width
                        }
                        scrollList ?: run {
                            Renderable.scrollList(
                                changelogList.map {
                                    if (it.startsWith("§l§9Version")) {
                                        Renderable.string(it, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER)
                                    } else {
                                        Renderable.wrappedString(it, width)
                                    }
                                },
                                height,
                                velocity = 6.0,
                                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                                scrollValue = changelogScroll,
                                button = 0
                            ).also {
                                scrollList = it
                            }
                        }
                    }.renderXYAligned(0, 0, width, height)
                }
                GlStateManager.translate(-xTranslate.toFloat(), -yTranslate.toFloat(), 0f)
            }
        }
    }

    private var changelogList: List<String> = emptyList()

    private fun getChangelog() {
        if (changelogList.isNotEmpty()) return
        SkyHanniMod.coroutineScope.launch {
            val url = "https://api.github.com/repos/hannibal002/SkyHanni/releases"
            val jsonObject = withContext(dispatcher) { APIUtil.getJSONResponseAsElement(url, apiName = "github") }
            val data = ConfigManager.gson.fromJson<List<ChangelogJson>>(jsonObject)
            val splitCurrent = currentVersion.split('.').filter { it != "Beta" }.map { it.toInt() }
            val splitTarget =
                UpdateManager.getNextVersion()?.split('.')?.filter { it != "Beta" }?.map { it.toInt() } ?: emptyList()
            val neededData = data.filter {
                val sub = it.tagName.split('.').filter { it != "Beta" }.map { it.toInt() }

                val first = (splitCurrent.getOrNull(0) ?: -1) to (sub.getOrNull(0) ?: -1)
                val second = (splitCurrent.getOrNull(1) ?: -1) to (sub.getOrNull(1) ?: -1)
                val third = (splitCurrent.getOrNull(2) ?: -1) to (sub.getOrNull(2) ?: -1)

                if (first.first > first.second) return@filter false
                if (first.first < first.second && second.second == -1) return@filter true
                if (second.first > second.second) return@filter false
                if (second.first < second.second && third.second == -1) return@filter true
                if (third.first > third.second) return@filter false
                if (third.first == third.second) return@filter false
                return@filter true
            }.dropLast(1)
            val formatted = neededData.map {
                it.body.replace(
                    "(- [^-]*)\\(https://github[\\w/.?$&#]*\\)".toRegex(), "§b$1"
                ) // Remove github link + color contributors
                    .replace("\r\n### Technical Details[^#]*".toRegex(), "\r\n") // Remove Technical Details
                    .replace("#+\\s*".toRegex(), "§l§9") // Formatting for headings
                    .replace("(\n[ \t]+)\\+".toRegex(), "$1§7") // Formatting for sub points
                    .replace("\n\\+".toRegex(), "\n§a") // Formatting for points
                    .split("\r\n") // Split at newlines
                    .let {// Change §a to §c if in removed
                        val index = it.indexOf("§l§9Removed Features")
                        if (index != -1) {
                            it.subList(0, index) + it.subList(index, it.size).map {
                                it.replace("§a", "§c")
                            }
                        } else {
                            it
                        }
                    }
            }
            changelogList = formatted.flatten().map { it.trimEnd() }
            formatted.forEach {// TODO remove
                it.forEach {
                    //ChatUtils.chat(it, prefix = false, prefixColor = "")
                    LorenzDebug.log(it)
                }
            }
        }
    }

    override fun keyboardInput(): Boolean {
        return false
    }

    override fun fulfillsSearch(word: String): Boolean {
        return super.fulfillsSearch(word) || word in "download" || word in "update"
    }
}
