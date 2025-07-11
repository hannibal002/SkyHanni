package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.containsKeys
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyhanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.WrappedStringRenderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.system.ModVersion
import java.util.NavigableMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ChangeLogViewerScreen : SkyhanniBaseScreen() {
    private val changelogScroll = ScrollValue()

    private lateinit var scrollList: Renderable
    private var lastWidth: Int = 0
    private var lastHeight: Int = 0

    private val buttonPanel = HorizontalContainerRenderable(
        listOf(
            Renderable.darkRectButton(
                StringRenderable("Include Betas"),
                startState = ChangelogViewer.shouldShowBeta,
                onClick = {
                    ChangelogViewer.shouldShowBeta = it
                    ChangelogViewer.shouldMakeNewList = true
                },
            ),
            Renderable.darkRectButton(
                StringRenderable("Show Technical Details"),
                startState = ChangelogViewer.showTechnicalDetails,
                onClick = {
                    ChangelogViewer.showTechnicalDetails = it
                    ChangelogViewer.shouldMakeNewList = true
                },
            ),
        ),
        10, RenderUtils.HorizontalAlignment.RIGHT,
        RenderUtils.VerticalAlignment.TOP,
    )

    override fun guiClosed() {
        DelayedRun.runDelayed(30.0.minutes) {
            if (ChangelogViewer.openTime.passedSince() > 20.0.minutes) {
                ChangelogViewer.cache.clear()
            }
        }
    }

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        ChangelogViewer.openTime = SimpleTimeMark.now()
        val width = 4 * this.width / 5
        val height = 4 * this.height / 5
        val xTranslate = this.width / 10
        val yTranslate = this.height / 10

        drawDefaultBackground(mouseX, mouseY, partialTicks)
        DrawContextUtils.translate(xTranslate - 2.0, yTranslate - 2.0, 0.0)
        GuiRenderUtils.drawFloatingRectDark(0, 0, width, height)
        DrawContextUtils.translate(-(xTranslate - 2.0), -(yTranslate - 2.0), 0.0)

        DrawContextUtils.translate(xTranslate.toFloat(), yTranslate.toFloat() + 5, 0f)
        Renderable.withMousePosition(mouseX - xTranslate, mouseY - yTranslate) {
            if (!ChangelogViewer.cache.containsKeys(ChangelogViewer.startVersion, ChangelogViewer.endVersion)) {
                ChangelogViewer.shouldMakeNewList = true
                StringRenderable(
                    if (ChangelogViewer.openTime.passedSince() >= 5.0.seconds)
                        "§aStill Loading. §cThe Version you are looking for may not exist"
                    else "§aStill Loading",
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                    verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                )
            } else {
                if (ChangelogViewer.shouldMakeNewList || lastWidth != width || lastHeight != height) {
                    lastWidth = width
                    lastHeight = height
                    val changelogList = (
                        ChangelogViewer.cache.subMap(
                            ChangelogViewer.startVersion,
                            false,
                            ChangelogViewer.endVersion,
                            true,
                        ).takeIf { it.isNotEmpty() } ?: ChangelogViewer.cache.subMap(
                            ChangelogViewer.startVersion,
                            true,
                            ChangelogViewer.endVersion,
                            true,
                        ) // If startVersion == endVersion
                        ).descendingMap()
                    scrollList = makeScrollList(changelogList, width, height)
                }
                scrollList
            }.renderXYAligned(0, 0, width, height)
            DrawContextUtils.translate(0f, -5f, 0f)
            val topOfGui = -buttonPanel.height - 5
            DrawContextUtils.translate(0f, topOfGui.toFloat(), 0f)
            buttonPanel.renderXAligned(0, topOfGui, width)
            Renderable.drawInsideDarkRect(
                StringRenderable("§9${ChangelogViewer.startVersion} §e➜ §9${ChangelogViewer.endVersion}"),
                horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
            ).renderXAligned(0, topOfGui, width)
            DrawContextUtils.translate(0f, -topOfGui.toFloat(), 0f)
        }
        DrawContextUtils.translate(-xTranslate.toFloat(), -yTranslate.toFloat(), 0f)
    }

    private fun makeScrollList(
        changelogList: NavigableMap<ModVersion, Map<String, List<String>>>,
        width: Int,
        height: Int,
    ): Renderable = Renderable.scrollList(
        changelogList.filter { ChangelogViewer.shouldShowBeta || !it.key.isBeta }.map { (version, body) ->
            listOf(
                StringRenderable("§l§9Version $version", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER),
            ) + makeChangeLogToRenderable(body, width) + listOf(
                Renderable.placeholder(
                    0, 15,
                ),
            )
        }.flatten().transformIf(
            { isEmpty() },
            {
                listOf(
                    if (changelogList.isEmpty()) {
                        StringRenderable("§aNo changes found", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER)
                    } else if (!ChangelogViewer.shouldShowBeta) {
                        StringRenderable(
                            "§aOnly Betas where added, turn on \"Include Betas\"",
                            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                        )
                    } else {
                        ErrorManager.skyHanniError(
                            "Idk how you ended up here",
                            "changelog" to changelogList,
                            "transformed" to this,
                            "show beta" to ChangelogViewer.shouldShowBeta,
                        )
                    },
                )
            },
        ),
        height,
        velocity = 12.0,
        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        scrollValue = changelogScroll,
        button = 0,
    )

    private fun makeChangeLogToRenderable(
        it: Map<String, List<String>>,
        width: Int,
    ) = it.mapNotNull { (key, value) ->
        if (!ChangelogViewer.showTechnicalDetails && key == "§l§9Technical Details") {
            return@mapNotNull null
        }
        value.map {
            WrappedStringRenderable(it, width)
        }
    }.flatten()
}
