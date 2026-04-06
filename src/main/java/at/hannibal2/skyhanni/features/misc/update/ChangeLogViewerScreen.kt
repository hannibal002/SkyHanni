package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.containsKeys
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.SkyHanniChromeScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.system.ModVersion
import java.util.NavigableMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ChangeLogViewerScreen : SkyHanniChromeScreen() {

    override val screenTitle = "Changelog"

    private val changelogScroll = ScrollValue()

    private lateinit var scrollList: Renderable
    private var cacheWasReady = false

    override fun onChromeDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val ready = ChangelogViewer.cache.containsKeys(ChangelogViewer.startVersion, ChangelogViewer.endVersion)
        if (ready && !cacheWasReady) {
            cacheWasReady = true
            rebuildDisplay()
        }
    }

    private val buttonPanel = Renderable.horizontal(
        Renderable.darkRectButton(
            Renderable.text("Include Betas"),
            startState = ChangelogViewer.shouldShowBeta,
            onClick = {
                ChangelogViewer.shouldShowBeta = it
                ChangelogViewer.shouldMakeNewList = true
                rebuildDisplay()
            },
        ),
        Renderable.darkRectButton(
            Renderable.text("Show Technical Details"),
            startState = ChangelogViewer.showTechnicalDetails,
            onClick = {
                ChangelogViewer.showTechnicalDetails = it
                ChangelogViewer.shouldMakeNewList = true
                rebuildDisplay()
            },
        ),
        spacing = 10,
        horizontalAlign = RenderUtils.HorizontalAlignment.RIGHT,
    )

    override fun guiClosed() {
        DelayedRun.runDelayed(30.0.minutes) {
            if (ChangelogViewer.openTime.passedSince() > 20.0.minutes) {
                ChangelogViewer.cache.clear()
            }
        }
    }

    override fun buildContent(): Renderable {
        ChangelogViewer.openTime = SimpleTimeMark.now()

        val cacheContainsKey = ChangelogViewer.cache.containsKeys(ChangelogViewer.startVersion, ChangelogViewer.endVersion)
        if (cacheContainsKey && (ChangelogViewer.shouldMakeNewList || !::scrollList.isInitialized)) {
            ChangelogViewer.shouldMakeNewList = false
            scrollList = makeScrollList(buildChangelogMap(), 400, 300)
        }

        val content = if (!cacheContainsKey) Renderable.text(
            if (ChangelogViewer.openTime.passedSince() >= 5.0.seconds)
                "§aStill Loading. §cThe Version you are looking for may not exist"
            else "§aStill Loading",
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
        ) else scrollList

        return Renderable.vertical(
            listOf(
                buttonPanel,
                Renderable.drawInsideDarkRect(
                    Renderable.text("§9${ChangelogViewer.startVersion} §e➜ §9${ChangelogViewer.endVersion}"),
                    horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
                ),
                content,
            ),
            spacing = 5,
        )
    }

    private fun buildChangelogMap(): NavigableMap<ModVersion, Map<String, List<String>>> = with(ChangelogViewer) {
        val changelogMap = cache.subMap(startVersion, false, endVersion, true).takeIfNotEmpty()
            ?: cache.subMap(startVersion, true, endVersion, true)
        return changelogMap.descendingMap()
    }

    @Suppress("SameParameterValue")
    private fun makeScrollList(
        changelogList: NavigableMap<ModVersion, Map<String, List<String>>>,
        width: Int,
        height: Int,
    ): Renderable = Renderable.scrollList(
        changelogList.filter { ChangelogViewer.shouldShowBeta || !it.key.isBeta }.map { (version, body) ->
            listOf(
                Renderable.text("§l§9Version $version", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER),
            ) + makeChangeLogToRenderable(body, width) + listOf(
                Renderable.placeholder(0, 15),
            )
        }.flatten().transformIf(
            { isEmpty() },
            {
                listOf(
                    if (changelogList.isEmpty()) {
                        Renderable.text("§aNo changes found", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER)
                    } else if (!ChangelogViewer.shouldShowBeta) Renderable.text(
                        "§aNo Full Releases in specified interval, modify the search or turn on \"Include Betas\"",
                        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                    ) else ErrorManager.skyHanniError(
                        "Idk how you ended up here",
                        "changelog" to changelogList,
                        "transformed" to this,
                    ),
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
            Renderable.wrappedText(it, width)
        }
    }.flatten()
}
