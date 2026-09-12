package at.hannibal2.skyhanni.config.core.dependency

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.common.IMinecraft
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.KeyboardEvent
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import java.lang.reflect.Field
import kotlin.math.max

/**
 * Shows a "Used by" banner above a config option listing other options that depend on it
 * (directly or via a third-party main toggle). Reverse view of [GuiOptionEditorDependencies].
 *
 * Clicking the banner expands a sticky panel with one row per dependent option, including
 * its full description; clicking a row jumps to and opens that option in the config.
 */
class GuiOptionEditorUsedBy(
    private val base: GuiOptionEditor,
    private val field: Field,
) : GuiOptionEditor(base.getOption()), ConfigBannerProvider {
    private var bannerHeight = 0
    private var panelHeight = 0
    private var hoverTooltip: List<String>? = null
    private var expanded = false
    private var pendingJump: UsedByEntry? = null
    private val rowHits = mutableListOf<Pair<UsedByEntry, IntRange>>()
    private var showAllHit: IntRange? = null

    init {
        // Kick off the (single, cached) reverse-dependency scan
        UsedByResolver.findUsedBy(field.declaringClass, field.name)
    }

    override fun bannerOffset(): Int =
        bannerHeight + panelHeight + ((base as? ConfigBannerProvider)?.bannerOffset() ?: 0)

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        val usedBy = UsedByResolver.findUsedBy(field.declaringClass, field.name)
        if (usedBy.isEmpty()) {
            bannerHeight = 0
            panelHeight = 0
            hoverTooltip = null
            rowHits.clear()
            base.render(context, x, y, width)
            return
        }
        val font = IMinecraft.INSTANCE.defaultFontRenderer
        val pad = (base.height * 0.08f).toInt().coerceAtLeast(3)
        val bannerH = max(font.height + pad * 2, MIN_BANNER_HEIGHT)
        bannerHeight = bannerH
        val bannerBottom = y + bannerH
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), bannerBottom.toFloat(), if (expanded) USED_BY_BG_ACTIVE else USED_BY_BG)
        val text = ("§7Used by: §f" + usedBy.take(3).joinToString(", ") { "§e${it.name}" } +
            if (usedBy.size > 3) " §7+${usedBy.size - 3} more" else "" +
            " §7- §b" + if (expanded) "click to hide usages" else "click to show usages").asStructuredText()
        val ty = y + (bannerH - font.height) / 2
        context.drawStringScaledMaxWidth(text, font, x + pad, ty, true, width - pad * 2, TEXT_COLOR)
        hoverTooltip = buildList {
            add(if (expanded) "§7Used by (click to hide):" else "§7Used by (click to show usages):")
            usedBy.forEach { add(" - §f${it.name}") }
        }
        rowHits.clear()
        showAllHit = null
        var contentY = bannerBottom
        if (expanded) {
            val panelTop = contentY
            val rowPad = 3
            var cursorY = panelTop
            usedBy.take(MAX_ROWS).forEach { entry ->
                val rowTop = cursorY
                val name = entry.name
                val descLines = buildList {
                    add(name)
                    if (entry.description.isNotBlank()) {
                        font.splitText(entry.description.asStructuredText(), width - 12).take(MAX_DESC_LINES)
                            .forEach { add("§7" + it.text) }
                    }
                }
                val rowHeight = descLines.size * (font.height + 1) + rowPad * 2
                context.drawColoredRect(x.toFloat(), rowTop.toFloat(), (x + width).toFloat(), (rowTop + rowHeight).toFloat(), if (isRowHovered(x, width, rowTop, rowHeight)) ROW_HOVER else ROW_BG)
                descLines.forEachIndexed { i, line ->
                    context.drawStringScaledMaxWidth(
                        line.asStructuredText(), font, x + pad, rowTop + rowPad + i * (font.height + 1), true, width - pad * 2, TEXT_COLOR,
                    )
                }
                rowHits.add(entry to (rowTop until (rowTop + rowHeight)))
                cursorY += rowHeight + 1
            }
            if (usedBy.size > MAX_ROWS) {
                val moreRow = cursorY
                val moreHeight = font.height + rowPad * 2
                val isHover = isRowHovered(x, width, moreRow, moreHeight)
                context.drawColoredRect(
                    x.toFloat(), moreRow.toFloat(), (x + width).toFloat(), (moreRow + moreHeight).toFloat(),
                    if (isHover) ROW_HOVER else ROW_BG,
                )
                context.drawStringScaledMaxWidth(
                    "§7... ${usedBy.size - MAX_ROWS} more §b(click to show all in config)".asStructuredText(),
                    font, x + pad, moreRow + rowPad, true, width - pad * 2, TEXT_COLOR,
                )
                showAllHit = moreRow until (moreRow + moreHeight)
                cursorY += moreHeight + 1
            }
            panelHeight = cursorY - panelTop
        } else {
            panelHeight = 0
        }
        contentY += panelHeight
        base.render(context, x, contentY, width)

        // Deferred jump: only jump once our banner/panel heights are refreshed for this frame,
        // otherwise moulconfig's scroll target computation uses the stale (expanded) heights.
        pendingJump?.let { entry ->
            pendingJump = null
            ConfigUtils.openEditorForField(entry.owner, entry.fieldName)
        }
    }

    private fun isRowHovered(x: Int, width: Int, rowTop: Int, rowHeight: Int): Boolean {
        val mx = IMinecraft.INSTANCE.mouseX
        val my = IMinecraft.INSTANCE.mouseY
        return mx in x..(x + width) && my in rowTop until (rowTop + rowHeight)
    }

    override fun mouseInput(
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        mouseEvent: MouseEvent?,
    ): Boolean {
        if (mouseEvent is MouseEvent.Click && mouseEvent.mouseState && mouseEvent.mouseButton == 0) {
            if (bannerHeight > 0 && mouseY in y until (y + bannerHeight)) {
                expanded = !expanded
                return true
            }
            if (expanded) {
                showAllHit?.let { range ->
                    if (mouseY in range) {
                        ConfigUtils.openFilteredConfig(UsedByResolver.transitiveUsedBy(field.declaringClass, field.name))
                        return true
                    }
                }
                rowHits.firstOrNull { mouseY in it.second }?.let { (entry, _) ->
                    expanded = false
                    pendingJump = entry
                    return true
                }
            }
        }
        return base.mouseInput(x, y + bannerHeight + panelHeight, width, mouseX, mouseY, mouseEvent)
    }

    override fun mouseInputOverlay(
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        mouseEvent: MouseEvent?,
    ): Boolean = base.mouseInputOverlay(x, y + bannerHeight + panelHeight, width, mouseX, mouseY, mouseEvent)

    override fun keyboardInput(event: KeyboardEvent?): Boolean = base.keyboardInput(event)

    override fun getHeight(): Int = base.height + bannerHeight + panelHeight

    override fun renderOverlay(context: RenderContext, x: Int, y: Int, width: Int) {
        if (bannerHeight > 0) {
            val mx = IMinecraft.INSTANCE.mouseX
            val my = IMinecraft.INSTANCE.mouseY
            if (mx in x..(x + width) && my in y until (y + bannerHeight)) {
                hoverTooltip?.let { RenderableTooltips.setTooltipForRender(it.map(StringRenderable::from)) }
            } else if (expanded) {
                showAllHit?.let { range ->
                    if (my in range) {
                        RenderableTooltips.setTooltipForRender(
                            listOf(StringRenderable.from("§bOpen the config with only this option and its dependents")),
                        )
                    }
                }
            }
        }
        base.renderOverlay(context, x, y + bannerHeight + panelHeight, width)
    }

    companion object {
        private const val MIN_BANNER_HEIGHT = 16
        private const val MAX_ROWS = 3
        private const val MAX_DESC_LINES = 3
        private const val USED_BY_BG = 0x33232355
        private const val USED_BY_BG_ACTIVE = 0x55232377
        private const val ROW_BG = 0x24151521
        private const val ROW_HOVER = 0x40373757
        private const val TEXT_COLOR = -0x1
    }
}

/**
 * A single option that depends on some other config field, with the information needed
 * to render it in the "used by" panel and jump to it in the config.
 */
data class UsedByEntry(
    val name: String,
    val description: String,
    val owner: Class<*>,
    val fieldName: String,
)

/**
 * Builds and caches the reverse dependency map ("what requires this option") for all config fields.
 * The scan runs once on a background thread and walks the live config object graph, recording
 * dependents for both [FeatureDependencyRequirement] dependencies and [ThirdPartyDependency]
 * (third-party main toggles).
 */
object UsedByResolver {
    private val reverseMap = mutableMapOf<String, MutableList<UsedByEntry>>()
    @Volatile
    private var built = false
    @Volatile
    private var buildStarted = false
    private val lock = Any()

    /**
     * Returns the dependents of the given config field, or an empty list when nothing requires
     * it (or the scan has not finished yet).
     */
    fun findUsedBy(owner: Class<*>, fieldName: String): List<UsedByEntry> {
        if (!built) startBuild()
        if (!built) return emptyList()
        synchronized(lock) { return reverseMap["${owner.name}#$fieldName"]?.toList() ?: emptyList() }
    }

    private fun startBuild() {
        if (buildStarted) return
        synchronized(lock) {
            if (buildStarted) return
            buildStarted = true
            Thread({ build() }, "skyhanni-config-usedby-resolver").apply { isDaemon = true }.start()
        }
    }

    private fun build() {
        try {
            walkInstance(SkyHanniMod.feature, mutableSetOf(), 0)
        } catch (_: Throwable) {
            // partial results are still usable
        } finally {
            built = true
        }
    }

    private fun walkInstance(instance: Any, visited: MutableSet<Any>, depth: Int) {
        if (depth > 10 || instance in visited) return
        visited.add(instance)
        for (field in instance.javaClass.declaredFields) {
            try {
                field.isAccessible = true
            } catch (_: Throwable) {
                continue
            }
            recordFieldDependents(field)
            val value = try { field.get(instance) } catch (_: Throwable) { continue }
            if (value != null && value !== instance && !isScalar(value)) {
                walkInstance(value, visited, depth + 1)
            }
        }
    }

    private fun isScalar(value: Any): Boolean {
        val type = value.javaClass
        if (type.isPrimitive || type.isEnum) return true
        if (value is String || value is Number || value is Boolean || value is Char) return true
        if (value is Collection<*> || value is Map<*, *>) return true
        val name = type.name
        return name.startsWith("java.") || name.startsWith("kotlin.") ||
            name.startsWith("io.github.notenoughupdates.moulconfig")
    }

    private fun recordFieldDependents(field: Field) {
        // Only real config options are shown in the "used by" panel and can be jumped to;
        // internal fields (e.g. no @ConfigOption) are not config options and are ignored.
        if (field.getAnnotation(ConfigOption::class.java) == null) return
        val entry = entryFor(field)
        try {
            val requirements = FeatureDependencyResolver.resolve(field)
            requirements.groups.forEach { group ->
                group.dependencies.forEach { dep ->
                    when (val source = dep.source) {
                        is FeatureDependencyResolver.DependencySource.BooleanField ->
                            addReverse("${source.owner.name}#${source.fieldName}", entry)
                    }
                }
            }
        } catch (_: Throwable) {
            // ignore fields that cannot be resolved
        }
    }

    private fun entryFor(field: Field): UsedByEntry {
        val ann = field.getAnnotation(ConfigOption::class.java)
        val name = ann?.name?.takeIf { it.isNotBlank() } ?: field.name
        val description = ann?.desc?.replace("\\n", "\n") ?: ""
        return UsedByEntry(name, description, field.declaringClass, field.name)
    }

    private fun addReverse(key: String, target: UsedByEntry) {
        // A field never "uses" itself; the class-level @ThirdPartyDependency annotation
        // would otherwise register the main toggle as a dependent of itself.
        if (key == "${target.owner.name}#${target.fieldName}") return
        synchronized(lock) { reverseMap.getOrPut(key) { mutableListOf() }.add(target) }
    }

    /**
     * Returns all config option keys that transitively depend on the given option
     * (dependents of dependents, ...), including the option itself. Used by the
     * "show all dependents in config" filter.
     */
    fun transitiveUsedBy(owner: Class<*>, fieldName: String): Set<String> {
        if (!built) startBuild()
        val keys = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add("${owner.name}#$fieldName")
        while (queue.isNotEmpty()) {
            val key = queue.removeFirst()
            if (!keys.add(key)) continue
            val dependents = synchronized(lock) { reverseMap[key]?.toList() ?: emptyList() }
            dependents.forEach { queue.add("${it.owner.name}#${it.fieldName}") }
        }
        return keys
    }
}
