package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.other.ChangelogJson
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.CollectionUtils.containsKey
import at.hannibal2.skyhanni.utils.CollectionUtils.getOrNull
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.isInt
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.fromJson
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import java.util.NavigableMap
import java.util.TreeMap
import kotlin.time.Duration.Companion.minutes

object ChangelogViewer {

    private var dispatcher = Dispatchers.IO

    private val cache: NavigableMap<VersionTag, Map<String, List<String>>> = TreeMap()

    private var openTime = SimpleTimeMark.farPast()

    private lateinit var startVersion: VersionTag
    private lateinit var endVersion: VersionTag

    private var shouldMakeNewList = false

    private var shouldShowBeta = LorenzUtils.isBetaVersion()
    private var showTechnicalDetails = false

    private val primaryColor = LorenzColor.DARK_GRAY.toColor().addAlpha(218)
    private val primary2Color = LorenzColor.DARK_GRAY.toColor().darker().withAlpha(220)

    fun showChangelog(currentVersion: String, targetVersion: String) {
        getChangelog(currentVersion, targetVersion)
        openChangelog()
    }

    private fun openChangelog() {
        if (Minecraft.getMinecraft().currentScreen !is ChangeLogViewer) SkyHanniMod.screenToOpen = ChangeLogViewer()
    }

    class ChangeLogViewer : GuiScreen() {
        private val changelogScroll = ScrollValue()

        private lateinit var scrollList: Renderable
        private var lastWidth: Int = 0
        private var lastHeight: Int = 0

        private val buttonPanel = Renderable.horizontalContainer(
            listOf(
                Renderable.rectButton(Renderable.string("Include Beta's"),
                    activeColor = primaryColor,
                    startState = shouldShowBeta,
                    onClick = {
                        shouldShowBeta = it
                        shouldMakeNewList = true
                    }), Renderable.rectButton(Renderable.string("Show Technical Details"),
                    activeColor = primaryColor,
                    startState = showTechnicalDetails,
                    onClick = {
                        showTechnicalDetails = it
                        shouldMakeNewList = true
                    })

            ), 10, horizontalAlign = RenderUtils.HorizontalAlignment.RIGHT
        )

        override fun onGuiClosed() {
            super.onGuiClosed()
            DelayedRun.runDelayed(30.0.minutes) {
                if (openTime.passedSince() > 20.0.minutes) {
                    cache.clear()
                }
            }
        }

        override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
            openTime = SimpleTimeMark.now()
            super.drawScreen(mouseX, mouseY, partialTicks)
            val width = 4 * this.width / 5
            val height = 4 * this.height / 5
            val xTranslate = this.width / 10
            val yTranslate = this.height / 10
            RenderUtils.drawRoundGradientRect(
                xTranslate - 2, yTranslate - 2, width + 4, height + 4, primary2Color, primaryColor.rgb
            )
            GlStateManager.translate(xTranslate.toFloat(), yTranslate.toFloat(), 0f)
            Renderable.withMousePosition(mouseX - xTranslate, mouseY - yTranslate) {
                if (!cache.containsKey(startVersion, endVersion)) {
                    shouldMakeNewList = true
                    Renderable.string(
                        "§aStill Loading",
                        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                        verticalAlign = RenderUtils.VerticalAlignment.CENTER
                    )
                } else {
                    if (shouldMakeNewList || lastWidth != width || lastHeight != height) {
                        lastWidth = width
                        lastHeight = height
                        val changelogList = cache.subMap(startVersion, false, endVersion, true).descendingMap()
                        scrollList = makeScrollList(changelogList, width, height)
                    }
                    scrollList
                }.renderXYAligned(0, 0, width, height)
                GlStateManager.translate(0f, -buttonPanel.height - 5f, 0f)
                buttonPanel.renderXAligned(0, -buttonPanel.height - 5, width)
                GlStateManager.translate(0f, buttonPanel.height + 5f, 0f)
            }
            GlStateManager.translate(-xTranslate.toFloat(), -yTranslate.toFloat(), 0f)
        }

        private fun makeScrollList(
            changelogList: NavigableMap<VersionTag, Map<String, List<String>>>,
            width: Int,
            height: Int,
        ): Renderable = Renderable.scrollList(
            changelogList.filter { shouldShowBeta || !it.key.isBeta }.map { (version, body) ->
                listOf(
                    Renderable.string(
                        "§l§9Version $version", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
                    )
                ) + makeChangeLogToRenderable(body, width) + listOf(
                    Renderable.placeholder(
                        0, 15
                    )
                )
            }.flatten().transformIf({ isEmpty() }, {
                listOf(
                    if (changelogList.isEmpty()) {
                        Renderable.string(
                            "§aNo changes found", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
                        )
                    } else if (!shouldShowBeta) {
                        Renderable.string(
                            "§aOnly Betas where added, turn on \"Include Beta's\"",
                            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
                        )
                    } else {
                        ErrorManager.skyHanniError(
                            "Idk how you ended up here",
                            "changelog" to changelogList,
                            "transformed" to this,
                            "show beta" to shouldShowBeta
                        )
                    }
                )
            }),
            height,
            velocity = 12.0,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            scrollValue = changelogScroll,
            button = 0
        )
    }

    private fun makeChangeLogToRenderable(
        it: Map<String, List<String>>,
        width: Int,
    ) = it.mapNotNull { (key, value) ->
        if (!showTechnicalDetails && key == "§l§9Technical Details") {
            return@mapNotNull null
        }
        value.map {
            Renderable.wrappedString(
                it, width
            )
        }
    }.flatten()

    private data class VersionTag(
        val first: Int,
        val second: Int,
        val third: Int,
        val fourth: Int,
        val isBeta: Boolean,
    ) : Comparable<VersionTag> {

        constructor(l: List<Int>, beta: Boolean) : this(
            l.getOrNull(0) ?: -1, l.getOrNull(1) ?: -1, l.getOrNull(2) ?: -1, l.getOrNull(3) ?: -1, beta
        )

        override operator fun compareTo(other: VersionTag): Int {
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

    /** Inclusive for both borders */
    private fun VersionTag.isInBetween(current: VersionTag, target: VersionTag): Boolean {
        if (this > target) return false
        if (this < current) return false
        if (this == current) return true
        return true
    }

    private fun String.toVersionTag(): VersionTag {
        val split = this.split('.')
        val ints = split.filter { it.isInt() }.map { it.toInt() }
        return VersionTag(ints, split.contains("Beta"))
    }

    private fun getChangelog(currentVersion: String, targetVersion: String) {
        startVersion = currentVersion.toVersionTag()
        endVersion = targetVersion.toVersionTag()
        if (cache.containsKey(startVersion, endVersion)) return
        SkyHanniMod.coroutineScope.launch {
            try {
                val url = "https://api.github.com/repos/hannibal002/SkyHanni/releases?per_page=100&page="
                val data = mutableListOf<ChangelogJson>()
                var pageNumber = 1
                while (data.isEmpty() || data.last().tagName.toVersionTag() > startVersion) {
                    val jsonObject = withContext(dispatcher) {
                        APIUtil.getJSONResponseAsElement(
                            url + pageNumber, apiName = "github"
                        )
                    }
                    val page = ConfigManager.gson.fromJson<List<ChangelogJson>>(jsonObject)
                    data.addAll(page)
                    pageNumber++
                }
                val neededData = data.filter {
                    val sub = it.tagName.toVersionTag()
                    sub.isInBetween(startVersion, endVersion)
                }
                neededData.forEach {
                    var headline = 0
                    cache[it.tagName.toVersionTag()] = it.body.replace(
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
                        }.mapKeys { it.value.firstOrNull() ?: "" }.toMutableMap().also {// Change §a to §c if in removed
                            val key = "§l§9Removed Features"
                            val subgroup = it[key] ?: return@also
                            it[key] = subgroup.map {
                                it.replace("§a", "§c")
                            }
                        }.toMap()
                }
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(e, "Changelog Loading Failed")
            }
        }
    }
}
