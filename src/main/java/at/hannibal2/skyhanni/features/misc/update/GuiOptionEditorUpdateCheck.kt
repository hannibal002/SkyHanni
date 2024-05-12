package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.other.ChangelogJson
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.CollectionUtils.getOrNull
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.NumberUtil.isInt
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
                getChangelog(currentVersion, UpdateManager.getNextVersion()!!)
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
                                    listOf(
                                        Renderable.string(
                                            "§l§9Version " + it.first.toString(),
                                            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
                                        )
                                    ) + makeChangeLogToRenderable(it.second, width) + listOf(
                                        Renderable.placeholder(
                                            0,
                                            15
                                        )
                                    )
                                }.flatten(),
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

    private fun makeChangeLogToRenderable(
        it: Map<String, List<String>>,
        width: Int,
    ) = it.mapNotNull { (key, value) ->
        if (key == "§l§9Technical Details") {
            return@mapNotNull null
        }
        value.map {
            Renderable.wrappedString(
                it,
                width
            )
        }
    }.flatten()

    private var changelogList: List<Pair<VersionTag, Map<String, List<String>>>> = emptyList()

    private class VersionTag(
        val first: Int,
        val second: Int,
        val third: Int,
        val fourth: Int,
        val isBeta: Boolean,
    ) {

        constructor(l: List<Int>, beta: Boolean) : this(
            l.getOrNull(0) ?: -1,
            l.getOrNull(1) ?: -1,
            l.getOrNull(2) ?: -1,
            l.getOrNull(3) ?: -1,
            beta
        )

        operator fun compareTo(other: VersionTag): Int {
            val first = first.compareTo(other.first)
            if (first != 0) return first
            val second = second.compareTo(other.second)
            if (second != 0) return second
            val beta = -isBeta.compareTo(other.isBeta)
            if (beta != 0) return beta
            val third = third.compareTo(other.third)
            if (third != 0) return third
            return fourth.compareTo(other.fourth)
        }

        override fun toString(): String {
            return if (isBeta) {
                "$first" + if (second == -1) " Beta" else ".$second" + if (third == -1) " Beta" else " Beta $third" + if (fourth == -1) "" else ".$fourth"
            } else {
                "$first" + if (second == -1) "" else ".$second" + if (third == -1) "" else ".$third" + if (fourth == -1) "" else ".$fourth"
            }
        }
    }

    private fun VersionTag.shouldShow(current: VersionTag, target: VersionTag): Boolean {
        if (this > target) return false
        if (this < current) return false
        if (this == current) return false
        return true
    }

    private fun String.toVersionTag(): VersionTag {
        val split = this.split('.')
        val ints = split.filter { it.isInt() }.map { it.toInt() }
        return VersionTag(ints, split.contains("Beta"))
    }

    private fun getChangelog(currentVersion: String, targetVersion: String) {
        if (changelogList.isNotEmpty()) return
        SkyHanniMod.coroutineScope.launch {
            try {
                val splitCurrent = currentVersion.toVersionTag()
                val splitTarget = targetVersion.toVersionTag()

                val url = "https://api.github.com/repos/hannibal002/SkyHanni/releases?per_page=100&page="
                val data = mutableListOf<ChangelogJson>()
                var pageNumber = 1
                while (data.isEmpty() || data.last().tagName.toVersionTag() > splitCurrent) {
                    val jsonObject =
                        withContext(dispatcher) {
                            APIUtil.getJSONResponseAsElement(
                                url + pageNumber,
                                apiName = "github"
                            )
                        }
                    val page = ConfigManager.gson.fromJson<List<ChangelogJson>>(jsonObject)
                    data.addAll(page)
                    pageNumber++
                }
                val neededData = data.filter {
                    val sub = it.tagName.toVersionTag()
                    sub.shouldShow(splitCurrent, splitTarget)
                }.dropLast(1)
                val formatted = neededData.map {
                    var headline = 0
                    it.tagName.toVersionTag() to
                        it.body.replace(
                            "\\(https://github[\\w/.?$&#]*\\)".toRegex(), ""
                        ) // Remove GitHub link
                            .replace("(- [^-\r\n]*\r\n)".toRegex(), "§b$1") // Color contributors
                            //.replace("\r\n### Technical Details[^#]*".toRegex(), "\r\n") // Remove Technical Details
                            .replace("#+\\s*".toRegex(), "§l§9") // Formatting for headings
                            .replace("(\n[ \t]+)[+\\-*]".toRegex(), "$1§7") // Formatting for sub points
                            .replace("\n[+\\-*]".toRegex(), "\n§a") // Formatting for points
                            .replace("\\[(.+)\\]\\(.+\\)".toRegex(), "$1") // Random Links
                            .replace("§l§9Version[^\r\n]*\r\n".toRegex(), "") // Remove Version from Body
                            .replace("\\s*\r\n$".toRegex(), "") // Remove trailing empty Lines
                            .split("\r\n") // Split at newlines
                            .map { it.trimEnd() } // Remove trailing empty stuff
                            .groupBy {
                                if (it.startsWith("§l§9")) {
                                    headline++
                                }
                                headline
                            }.mapKeys { it.value.firstOrNull() ?: "" }
                            .toMutableMap()
                            .also {// Change §a to §c if in removed
                                val key = "§l§9Removed Features"
                                val subgroup = it[key] ?: return@also
                                it[key] = subgroup.map {
                                    it.replace("§a", "§c")
                                }
                            }.toMap()
                    // TODO caching formated for 30mins
                    // TODO notice technical details
                    // TODO toggle for only major
                    // TODO toggle for technical details
                }
                changelogList = formatted
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(e, "Changelog Loading Failed")
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
