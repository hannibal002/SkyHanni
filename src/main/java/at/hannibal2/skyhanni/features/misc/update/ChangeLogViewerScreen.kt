package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.CollectionUtils.containsKeys
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import java.util.NavigableMap
import kotlin.time.Duration.Companion.minutes

class ChangeLogViewerScreen : GuiScreen() {
    private val changelogScroll = ScrollValue()

    private lateinit var scrollList: Renderable
    private var lastWidth: Int = 0
    private var lastHeight: Int = 0

    private val buttonPanel = Renderable.horizontalContainer(
        listOf(
            Renderable.rectButton(Renderable.string("Include Beta's"),
                activeColor = ChangelogViewer.primaryColor,
                startState = ChangelogViewer.shouldShowBeta,
                onClick = {
                    ChangelogViewer.shouldShowBeta = it
                    ChangelogViewer.shouldMakeNewList = true
                }), Renderable.rectButton(Renderable.string("Show Technical Details"),
                activeColor = ChangelogViewer.primaryColor,
                startState = ChangelogViewer.showTechnicalDetails,
                onClick = {
                    ChangelogViewer.showTechnicalDetails = it
                    ChangelogViewer.shouldMakeNewList = true
                })

        ), 10, horizontalAlign = RenderUtils.HorizontalAlignment.RIGHT
    )

    override fun onGuiClosed() {
        super.onGuiClosed()
        DelayedRun.runDelayed(30.0.minutes) {
            if (ChangelogViewer.openTime.passedSince() > 20.0.minutes) {
                ChangelogViewer.cache.clear()
            }
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        ChangelogViewer.openTime = SimpleTimeMark.now()
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
            ChangelogViewer.primary2Color,
            ChangelogViewer.primaryColor.rgb
        )
        GlStateManager.translate(xTranslate.toFloat(), yTranslate.toFloat(), 0f)
        Renderable.withMousePosition(mouseX - xTranslate, mouseY - yTranslate) {
            if (!ChangelogViewer.cache.containsKeys(ChangelogViewer.startVersion, ChangelogViewer.endVersion)) {
                ChangelogViewer.shouldMakeNewList = true
                Renderable.string(
                    "§aStill Loading",
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                    verticalAlign = RenderUtils.VerticalAlignment.CENTER
                )
            } else {
                if (ChangelogViewer.shouldMakeNewList || lastWidth != width || lastHeight != height) {
                    lastWidth = width
                    lastHeight = height
                    val changelogList = (ChangelogViewer.cache.subMap(
                        ChangelogViewer.startVersion,
                        false,
                        ChangelogViewer.endVersion,
                        true
                    )
                        .takeIf { it.isNotEmpty() }
                        ?: ChangelogViewer.cache.subMap(
                            ChangelogViewer.startVersion,
                            true,
                            ChangelogViewer.endVersion,
                            true
                        ) // If startVersion == endVersion
                        ).descendingMap()
                    scrollList = makeScrollList(changelogList, width, height)
                }
                scrollList
            }.renderXYAligned(0, 0, width, height)
            GlStateManager.translate(0f, -buttonPanel.height - 5f, 0f)
            buttonPanel.renderXAligned(0, -buttonPanel.height - 5, width)
            Renderable.drawInsideRoundedRect(
                Renderable.string(
                    "§9${ChangelogViewer.startVersion} §e➜ §9${ChangelogViewer.endVersion}",
                ),
                ChangelogViewer.primaryColor,
                horizontalAlign = RenderUtils.HorizontalAlignment.LEFT
            ).renderXAligned(0, -buttonPanel.height - 5, width)
            GlStateManager.translate(0f, buttonPanel.height + 5f, 0f)
        }
        GlStateManager.translate(-xTranslate.toFloat(), -yTranslate.toFloat(), 0f)
    }

    private fun makeScrollList(
        changelogList: NavigableMap<ChangelogViewer.VersionTag, Map<String, List<String>>>,
        width: Int,
        height: Int,
    ): Renderable = Renderable.scrollList(
        changelogList.filter { ChangelogViewer.shouldShowBeta || !it.key.isBeta }.map { (version, body) ->
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
                } else if (!ChangelogViewer.shouldShowBeta) {
                    Renderable.string(
                        "§aOnly Betas where added, turn on \"Include Beta's\"",
                        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
                    )
                } else {
                    ErrorManager.skyHanniError(
                        "Idk how you ended up here",
                        "changelog" to changelogList,
                        "transformed" to this,
                        "show beta" to ChangelogViewer.shouldShowBeta
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

    private fun makeChangeLogToRenderable(
        it: Map<String, List<String>>,
        width: Int,
    ) = it.mapNotNull { (key, value) ->
        if (!ChangelogViewer.showTechnicalDetails && key == "§l§9Technical Details") {
            return@mapNotNull null
        }
        value.map {
            Renderable.wrappedString(
                it, width
            )
        }
    }.flatten()
}
