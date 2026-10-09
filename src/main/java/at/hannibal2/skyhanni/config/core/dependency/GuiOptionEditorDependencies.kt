package at.hannibal2.skyhanni.config.core.dependency

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import io.github.notenoughupdates.moulconfig.common.IMinecraft
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import java.lang.reflect.Field
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.jvm.isAccessible

/**
 * Renders dependency requirements on top of an option row.
 */
class GuiOptionEditorDependencies(
    private val base: GuiOptionEditor,
    private val initialRequirements: FeatureDependencyResolver.Requirements,
    private val dependencyField: Field? = null,
) : GuiOptionEditor(base.getOption()), ConfigBannerProvider {
    private data class ButtonHitbox(
        val sourceLabel: String,
        var x1: Int,
        var y1: Int,
        var x2: Int,
        var y2: Int,
        val labelText: String,
        val hasButton: Boolean = true,
        var rowY1: Int = 0,
        var rowY2: Int = 0,
    )

    override fun bannerOffset(): Int =
        bannerHeight + dependencyListHeight + ((base as? ConfigBannerProvider)?.bannerOffset() ?: 0)

    private val buttons = mutableListOf<ButtonHitbox>()
    private val rowButtons = mutableListOf<ButtonHitbox>()
    private var bannerHeight = MIN_BANNER_HEIGHT
    private var hoverTooltip: List<String>? = null
    private var blocked = true
    private var requirementsExpanded = false
    private var currentRequirements = initialRequirements
    private var dependencyListHeight = 0

    // caches to avoid expensive reflection/string work each frame
    private val ownerInstanceCache = mutableMapOf<Class<*>, Any?>()

    // per-frame satisfied cache to avoid stale values across renders; cleared at start of render
    private val frameSatisfied = mutableMapOf<FeatureDependencyResolver.Dependency, Boolean>()
    private var cachedBannerText: String? = null
    // dependencyField passed in constructor; if null, we'll rely on initialRequirements

    // Async loading state: avoid blocking UI while doing heavier reflection/resolution
    @Volatile
    private var requirementsResolved = false

    @Volatile
    private var loadingRequirements = false

    init {
        // estimate dependency list height before first render to avoid overlaying UI
        val estimatedRow = 14 // reasonable default for a font row
        val padPerRow = 4
        dependencyListHeight = initialRequirements.groups.sumOf { group ->
            // one line for group label
            val groupLines = 1
            val depLines = group.dependencies.size
            (groupLines + depLines) * (estimatedRow + padPerRow)
        }.coerceAtLeast(0)
        // schedule an async expansion later when first shown (lazy start)
    }

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        // Start async resolution on first render if not yet done
        if (!requirementsResolved && !loadingRequirements) {
            loadingRequirements = true
            Thread {
                try {
                    val baseReqs = dependencyField?.let { FeatureDependencyResolver.resolve(it) } ?: initialRequirements
                    val expanded = expandRequirementsRecursive(baseReqs)
                    synchronized(this) {
                        currentRequirements = expanded
                        cachedBannerText = null
                        // reset caches that may have been invalidated by new resolution
                        ownerInstanceCache.clear()
                        frameSatisfied.clear()
                        requirementsResolved = true
                        loadingRequirements = false
                    }
                } catch (_: Throwable) {
                    synchronized(this) {
                        // on any failure, fall back to initialRequirements and mark resolved so we don't retry frequently
                        currentRequirements = initialRequirements
                        requirementsResolved = true
                        loadingRequirements = false
                    }
                }
            }.start()
        }

        // If we're still resolving, show a small non-blocking banner and render base immediately
        if (!requirementsResolved) {
            // Show loading banner but do not block interaction with base content
            val font = IMinecraft.INSTANCE.defaultFontRenderer
            val pad = (base.height * 0.08f).toInt().coerceAtLeast(3)
            val loadingText = "§eLoading requirements..."
            val loadingH = max(font.height + BANNER_PADDING * 2, MIN_BANNER_HEIGHT)
            val bannerBottom = y + loadingH
            context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), bannerBottom.toFloat(), 0x33333300)
            val ty = y + (loadingH - font.height) / 2
            context.drawStringScaledMaxWidth(loadingText.asStructuredText(), font, x + pad, ty, true, width - pad * 2, TEXT_COLOR)
            // render base content immediately under the loading banner
            base.render(context, x, y + loadingH, width)
            return
        }

        // Normal (resolved) rendering path
        dependencyField?.let {
            // Prefer to re-resolve live but expand recursively so dependencies-of-dependencies are visible.
            // Only do this when we already performed the initial async resolution to avoid blocking UI on first open.
            if (requirementsResolved) {
                try {
                    currentRequirements = expandRequirementsRecursive(FeatureDependencyResolver.resolve(it))
                } catch (_: Throwable) {
                    // fallback: keep currentRequirements
                }
            }
        }
        // start of render: clear per-frame satisfied cache so we recompute current states and avoid stale values
        frameSatisfied.clear()
        buttons.clear()
        val font = IMinecraft.INSTANCE.defaultFontRenderer
        val pad = (base.height * 0.08f).toInt().coerceAtLeast(3)
        val satisfiedAnyGroup = currentRequirements.groups.any { group ->
            isGroupMet(group)
        }
        blocked = !satisfiedAnyGroup

        // Always reserve space for the requirements banner (collapsed + neutral when satisfied)
        val bannerH = max(font.height + BANNER_PADDING * 2, MIN_BANNER_HEIGHT)
        bannerHeight = bannerH
        val bannerBottom = y + bannerH
        val bannerText = if (blocked) {
            "§c⚠ ${cachedBannerText ?: buildBannerText().also { cachedBannerText = it }}"
        } else if (requirementsExpanded) {
            "§aRequirements met §7(click to collapse)"
        } else {
            "§aRequirements met §7(click to expand)"
        }
        val bgColor = if (blocked) BLOCKED_BG else SATISFIED_BG
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), bannerBottom.toFloat(), bgColor)
        val ty = y + (bannerH - font.height) / 2
        context.drawStringScaledMaxWidth(bannerText.asStructuredText(), font, x + pad, ty, true, width - pad * 2, TEXT_COLOR)

        // render dependency rows when blocked or manually expanded, then the base content
        val bodyY = y + bannerH
        val showList = blocked || requirementsExpanded
        layoutRowButtons(x, bodyY, width)
        if (showList) {
            renderDependencyList(context, x, bodyY, width)
        } else {
            dependencyListHeight = 0
        }
        val contentY = bodyY + dependencyListHeight
        base.render(context, x, contentY, width)
        // draw overlay to visually block interaction with base content.
        // the overlay is drawn below the requirement info header (banner + dependency rows) and is
        // extended downwards by that header offset so blocked options look clearly dimmed, while
        // staying capped so very tall editors (e.g. draggable lists) don't show a giant red area.
        if (blocked) {
            val overlayHeight = min(base.height, bannerHeight + dependencyListHeight + BLOCKED_OVERLAY_MAX_HEIGHT)
            context.drawColoredRect(
                x.toFloat(),
                contentY.toFloat(),
                (x + width).toFloat(),
                (contentY + overlayHeight).toFloat(),
                OVERLAY_BG,
            )
        }
    }

    private fun renderDependencyList(context: RenderContext, x: Int, y: Int, width: Int) {
        // layoutRowButtons should already have been called by the caller (render); avoid recomputing here to keep positions stable
        val font = IMinecraft.INSTANCE.defaultFontRenderer
        val pad = 4
        var cursorY = y
        val mx = IMinecraft.INSTANCE.mouseX
        val my = IMinecraft.INSTANCE.mouseY
        // Flatten dependencies under a single header; keep group messages only if non-blank and render them as small separators
        currentRequirements.groups.forEach { group ->
            if (group.message.isNotBlank()) {
                val gm = group.message.asStructuredText()
                context.drawStringScaledMaxWidth(gm, font, x + pad, cursorY, true, width - pad * 2, TEXT_COLOR)
                cursorY += font.height + pad
            }
            group.dependencies.forEach { dep ->
                val rowHeight = font.height + pad * 2
                val rowTop = cursorY
                val rowBottom = rowTop + rowHeight
                context.drawColoredRect(x.toFloat(), rowTop.toFloat(), (x + width).toFloat(), rowBottom.toFloat(), ROW_BG)
                val label = buildDependencyLabel(dep).asStructuredText()
                // Reserve space for the row button so the label never overlaps it
                val hb = rowButtons.firstOrNull { it.sourceLabel == dep.label }
                val labelMaxWidth = width - pad * 3 - (if (hb != null && hb.hasButton) hb.x2 - hb.x1 else 0) - 8
                context.drawStringScaledMaxWidth(label, font, x + pad, rowTop + pad, true, labelMaxWidth.coerceAtLeast(20), TEXT_COLOR)
                // draw the jump button using the layout computed earlier (every row gets one)
                if (hb != null && hb.hasButton) {
                    drawEnableButton(context, hb.x1, hb.y1, hb.x2 - hb.x1, hb.y2 - hb.y1, hb.labelText.asStructuredText(), mx, my)
                }
                // debug: draw hitbox rectangles/lines if enabled
                if (DEBUG_HITBOX) {
                    rowButtons.firstOrNull { it.sourceLabel == dep.label }?.let { hb ->
                        // semi-transparent red overlay for hitbox
                        context.drawColoredRect(hb.x1.toFloat(), (rowTop).toFloat(), hb.x2.toFloat(), (rowBottom).toFloat(), 0x44FF0000)
                        // green line at rowTop
                        context.drawColoredRect(
                            x.toFloat(),
                            rowTop.toFloat(),
                            (x + width).toFloat(),
                            (rowTop + 1).toFloat(),
                            0xFF00FF00.toInt(),
                        )
                        // blue line at button top (hb.y1)
                        context.drawColoredRect(
                            x.toFloat(),
                            hb.y1.toFloat(),
                            (x + width).toFloat(),
                            (hb.y1 + 1).toFloat(),
                            0xFF0000FF.toInt(),
                        )
                    }
                }
                cursorY += rowHeight + pad / 2
            }
            cursorY += pad
        }
        // dependencyListHeight already set by layoutRowButtons
    }

    private fun layoutRowButtons(x: Int, y: Int, width: Int) {
        val font = IMinecraft.INSTANCE.defaultFontRenderer
        val padList = 4
        rowButtons.clear()
        var cursorY = y
        currentRequirements.groups.forEach { group ->
            // only reserve space for a group header when a message is present (matches renderDependencyList)
            if (group.message.isNotBlank()) cursorY += font.height + padList
            group.dependencies.forEach { dep ->
                val rowHeight = font.height + padList * 2
                val rowTop = cursorY
                // Every row stays clickable to jump to the option, even when satisfied.
                // The Enable button is only offered for unsatisfied non-third-party dependencies
                // (a server integration's main toggle can't be enabled unknowingly).
                if (!isSatisfied(dep)) {
                    val label = "§fEnable"
                    val btnW = font.getStringWidth(label) + 8
                    val buttonWidth = btnW.coerceAtLeast(font.height + 8)
                    val btnX = x + width - buttonWidth - padList
                    val btnH = font.height + 4
                    val btnY = rowTop + (rowHeight - btnH) / 2
                    val hb = ButtonHitbox(dep.label, btnX, btnY, btnX + buttonWidth, btnY + btnH, label)
                    hb.rowY1 = rowTop
                    hb.rowY2 = rowTop + rowHeight
                    rowButtons.add(hb)
                } else {
                    val hb = ButtonHitbox(dep.label, 0, 0, 0, 0, "", hasButton = false)
                    hb.rowY1 = rowTop
                    hb.rowY2 = rowTop + rowHeight
                    rowButtons.add(hb)
                }
                cursorY += rowHeight + padList / 2
            }
            cursorY += padList
        }
        dependencyListHeight = (cursorY - y).coerceAtLeast(0)
    }

    private fun buildDependencyLabel(dep: FeatureDependencyResolver.Dependency): String {
        val state = if (isSatisfied(dep)) "§aEnabled" else "§cDisabled"
        return when (val source = dep.source) {
            is FeatureDependencyResolver.DependencySource.BooleanField -> if (isMainToggleField(source)) "§7requires \"Main toggle - $state\""
            else "${dep.label} - $state"
        }
    }

    /**
     * True when the dependency is a feature's main toggle (a boolean field annotated with
     * [at.hannibal2.skyhanni.config.FeatureToggle]). For those the generic "Main toggle" label is shown instead of the config
     * option name, which is often just "Enabled" and reads confusingly next to the state.
     */
    private fun isMainToggleField(source: FeatureDependencyResolver.DependencySource.BooleanField): Boolean {
        val field = runCatching { source.owner.getDeclaredField(source.effectiveFieldName) }.getOrNull() ?: return false
        return field.getAnnotation(FeatureToggle::class.java) != null
    }

    /**
     * A requirement group counts as met only when every third-party main toggle in it is
     * satisfied; an unsatisfied main toggle must never make the group look green.
     */
    private fun isGroupMet(group: FeatureDependencyResolver.RequirementGroup): Boolean {
        return group.dependencies.filter { false }.all { isSatisfied(it) } && if (group.requireAll) {
            group.dependencies.all { isSatisfied(it) }
        } else {
            group.dependencies.any { isSatisfied(it) }
        }
    }

    private fun drawEnableButton(
        context: RenderContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        label: StructuredText,
        mouseX: Int = -1,
        mouseY: Int = -1,
    ) {
        // improved visuals: subtle rounded-ish look via 1px darker border and lighter top
        val bg = BUTTON_BG
        val border = BUTTON_BORDER
        val topHighlight = BUTTON_HIGHLIGHT
        // fill
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), bg)
        // top highlight
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + 1).toFloat(), topHighlight)
        // border
        context.drawColoredRect(x.toFloat(), (y + height - 1).toFloat(), (x + width).toFloat(), (y + height).toFloat(), border)
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + 1).toFloat(), (y + height).toFloat(), border)
        context.drawColoredRect((x + width - 1).toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), border)

        val font = IMinecraft.INSTANCE.defaultFontRenderer
        // slightly more padding for nicer look; use contrasting text color
        // use smaller left/right padding so button is not larger than needed
        context.drawStringScaledMaxWidth(label, font, x + 4, y + (height - font.height) / 2, true, width - 8, TEXT_COLOR)

        // hover effect (subtle overlay)
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), BUTTON_HOVER_OVERLAY)
        }

        // note: we don't mutate the shared `buttons` list here; hitboxes are recomputed centrally when needed
    }

    private fun isSatisfied(dep: FeatureDependencyResolver.Dependency): Boolean {
        // use satisfied cache if available (avoid recomputing during same frame)
        frameSatisfied[dep]?.let { return it }
        val result = when (val source = dep.source) {

            is FeatureDependencyResolver.DependencySource.BooleanField -> inspectBooleanField(source)
        }
        // store in per-frame cache
        frameSatisfied[dep] = result
        return result
    }

    private fun inspectBooleanField(fieldSource: FeatureDependencyResolver.DependencySource.BooleanField): Boolean {
        return try {
            // prefer object singleton if available (cheap). Only deep-search when necessary.
            val obj = fieldSource.owner.kotlin.objectInstance
            val inst =
                if (obj != null) obj else runCatching {
                    ownerInstanceCache[fieldSource.owner]
                        ?: findExistingInstance(fieldSource.owner)?.also { ownerInstanceCache[fieldSource.owner] = it }
                }.getOrNull()
            val instance = inst ?: runCatching { fieldSource.owner.newInstance() }.getOrNull()
            if (instance == null) return false
            fieldSource.getter(instance)
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Try to find an existing live instance of the given type in known roots (SkyHanniMod fields).
     */
    private fun deepFindInstance(root: Any, target: Class<*>, visited: MutableSet<Any>, depth: Int = 0): Any? {
        if (depth > 8) return null
        if (target.isInstance(root)) return root
        if (root in visited) return null
        visited.add(root)
        for (f in root.javaClass.declaredFields) {
            try {
                f.isAccessible = true
                val v = f.get(root) ?: continue
                if (target.isInstance(v)) return v
                val found = deepFindInstance(v, target, visited, depth + 1) ?: continue
                return found
            } catch (_: Throwable) {
                // ignore
            }
        }
        return null
    }

    private fun findExistingInstance(owner: Class<*>): Any? {
        // check primary features object
        val features = SkyHanniMod.feature
        deepFindInstance(features, owner, mutableSetOf())?.let { return it }
        // fallback: check top-level SkyHanniMod static fields
        if (owner.isInstance(features)) return features
        for (f in SkyHanniMod::class.java.declaredFields) {
            try {
                f.isAccessible = true
                val v = f.get(null) ?: continue
                if (owner.isInstance(v)) return v
            } catch (_: Throwable) {
                // ignore
            }
        }
        return null
    }

    override fun mouseInput(
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        mouseEvent: MouseEvent?,
    ): Boolean {
        val isClick = mouseEvent is MouseEvent.Click && mouseEvent.mouseState
        // recompute state so hitboxes match the current render
        updateLayout(x, y, width)
        // compute the full height reserved for this editor (banner + dependency list + base)
        val totalBannerAndList = bannerHeight + dependencyListHeight
        val dependencyZoneBottom = y + totalBannerAndList
        val insideDependencyZone = mouseX in x..(x + width) && mouseY in y..dependencyZoneBottom
        if (isClick && insideDependencyZone) {
            // Banner click: when requirements are met, toggle the collapsed/expanded state
            if (mouseY <= y + bannerHeight && !blocked) {
                requirementsExpanded = !requirementsExpanded
                return true
            }
            // Click on a dependency row button: enable the dependency option in the config
            rowButtons.firstOrNull { it.hasButton && mouseX in it.x1..it.x2 && mouseY in it.y1..it.y2 }?.let { hit ->
                findCurrentDependencyByLabel(hit.sourceLabel)?.let { dep -> enableDependency(dep) }
                return true
            }
            // Click anywhere else on a dependency row: jump to the option in the config
            rowButtons.firstOrNull { mouseY in it.rowY1..it.rowY2 && mouseX in x..(x + width) }?.let { hit ->
                findCurrentDependencyByLabel(hit.sourceLabel)?.let { dep -> jumpToDependency(dep) }
                return true
            }
            if (blocked) return true
        }
        if (blocked) {
            // clicks outside the dependency zone are not consumed; base stays blocked
            return false
        }
        // forward remaining mouse events to base editor, adjusting y to the base content top
        val contentTop = y + totalBannerAndList
        return base.mouseInput(x, contentTop, width, mouseX, mouseY, mouseEvent)
    }

    private fun jumpToDependency(dep: FeatureDependencyResolver.Dependency) {
        when (val s = dep.source) {
            is FeatureDependencyResolver.DependencySource.BooleanField -> {
                ConfigUtils.openEditorForField(s.owner, s.effectiveFieldName)
            }
        }
    }

    /**
     * Enables the option a dependency refers to directly. Falls back to jumping to the option
     * in the config if the value cannot be set programmatically.
     */
    private fun enableDependency(dep: FeatureDependencyResolver.Dependency) {
        when (val s = dep.source) {
            is FeatureDependencyResolver.DependencySource.BooleanField -> {
                if (enableBooleanField(s)) {
                    SkyHanniMod.configManager.saveConfig(FEATURES, "dependency-unlock")
                    SkyHanniMod.configManager.recreateConfig()
                    afterEnable(dep)
                } else {
                    // cannot set the value programmatically, so jump to the option instead
                    jumpToDependency(dep)
                }
            }
        }
    }

    private fun enableBooleanField(source: FeatureDependencyResolver.DependencySource.BooleanField): Boolean {
        source.enabler?.let { enabler ->
            val instance = runCatching { source.owner.kotlin.objectInstance ?: findExistingInstance(source.owner) }.getOrNull()
            if (instance != null) {
                return runCatching {
                    enabler(instance)
                    true
                }.getOrElse { false }
            }
        }
        val property = source.property
        if (property != null) {
            val instance = source.owner.kotlin.objectInstance
                ?: runCatching { findExistingInstance(source.owner) }.getOrNull()
            if (instance != null) {
                return runCatching {
                    property.isAccessible = true
                    property.set(instance, true)
                    true
                }.getOrElse { false }
            }
        }
        val javaField = runCatching { source.owner.getDeclaredField(source.fieldName) }.getOrNull()
        if (javaField != null &&
            (javaField.type == Boolean::class.javaPrimitiveType || javaField.type == Boolean::class.javaObjectType)
        ) {
            val instance = runCatching { source.owner.kotlin.objectInstance ?: findExistingInstance(source.owner) }.getOrNull()
            if (instance != null) {
                return runCatching {
                    javaField.isAccessible = true
                    javaField.setBoolean(instance, true)
                    true
                }.getOrElse { false }
            }
        }
        return false
    }

    private fun afterEnable(dep: FeatureDependencyResolver.Dependency) {
        // invalidate per-frame state so the next render reflects the new value
        frameSatisfied.clear()
        ownerInstanceCache.clear()
        hoverTooltip = listOf("§aEnabled ${dep.label}", "§7Click to jump to this option")
    }

    // Ensure parent layout reserves space for our banner + dependency list
    override fun getHeight(): Int {
        // compute whether we'd be blocked to determine reserved height; keep same resolution logic as render
        // Avoid heavy synchronous resolution on initial load: if we haven't resolved async yet, rely on cached currentRequirements
        val reqs = if (!requirementsResolved) currentRequirements else (dependencyField?.let {
            try {
                expandRequirementsRecursive(FeatureDependencyResolver.resolve(it))
            } catch (_: Throwable) {
                currentRequirements
            }
        } ?: currentRequirements)
        val satisfiedAnyGroup = reqs.groups.any { group ->
            isGroupMet(group)
        }
        val showList = !satisfiedAnyGroup || requirementsExpanded
        return base.height + bannerHeight + (if (showList) dependencyListHeight else 0)
    }

    /**
     * Recompute requirements state, banner/list heights and row button hitboxes.
     * Called from render and mouseInput so both stay in sync.
     */
    private fun updateLayout(x: Int, y: Int, width: Int) {
        if (requirementsResolved) {
            dependencyField?.let {
                currentRequirements = try {
                    expandRequirementsRecursive(FeatureDependencyResolver.resolve(it))
                } catch (_: Throwable) {
                    currentRequirements
                }
            }
        }
        frameSatisfied.clear()
        val font = IMinecraft.INSTANCE.defaultFontRenderer
        val satisfiedAnyGroup = currentRequirements.groups.any { group ->
            isGroupMet(group)
        }
        blocked = !satisfiedAnyGroup
        bannerHeight = max(font.height + BANNER_PADDING * 2, MIN_BANNER_HEIGHT)
        rowButtons.clear()
        buttons.clear()
        if (blocked || requirementsExpanded) {
            layoutRowButtons(x, y + bannerHeight, width)
        } else {
            dependencyListHeight = 0
        }
        buttons.addAll(rowButtons)
    }

    private fun buildBannerText(): String {
        // Simplified header: indicate whether requirements are 'all' (every dependency in single group) or 'any'
        if (currentRequirements.groups.isEmpty()) return "Requires"
        val needsAll = currentRequirements.groups.size == 1 && currentRequirements.groups[0].requireAll
        return if (needsAll) "§8Requires: §f(all)" else "§8Requires: §f(any)"
    }

    override fun mouseInputOverlay(
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        mouseEvent: MouseEvent?,
    ): Boolean {
        // forward overlay mouse events to base with Y offset of banner
        val bannerAndList = bannerHeight + dependencyListHeight
        return base.mouseInputOverlay(x, y + bannerAndList, width, mouseX, mouseY, mouseEvent)
    }

    override fun renderOverlay(context: RenderContext, x: Int, y: Int, width: Int) {
        // show tooltip for banner when hovering; if hoverTooltip set (e.g. after enabling) show it, otherwise compute from requirements
        val tips = hoverTooltip ?: buildList {
            add("§7Dependencies")
            currentRequirements.groups.forEach { group ->
                add((group.message.ifBlank { "Requires" }))
                group.dependencies.forEach {
                    add(
                        " - ${
                            when (val s = it.source) {
                                is FeatureDependencyResolver.DependencySource.BooleanField -> it.label
                            }
                        }",
                    )
                }
            }
        }
        val mx = IMinecraft.INSTANCE.mouseX
        val my = IMinecraft.INSTANCE.mouseY
        val bannerBottom = y + bannerHeight
        when {
            mx in x..(x + width) && my in y..bannerBottom -> {
                RenderableTooltips.setTooltipForRender(tips.map(StringRenderable::from))
            }
            (blocked || requirementsExpanded) -> {
                rowButtons.firstOrNull { my in it.rowY1..it.rowY2 && mx in x..(x + width) }?.let { hb ->
                    val tooltip = if (hb.hasButton && mx in hb.x1..hb.x2 && my in hb.y1..hb.y2) {
                        listOf("§7Click to enable this option")
                    } else {
                        listOf("§7Click to jump to this option")
                    }
                    RenderableTooltips.setTooltipForRender(tooltip.map(StringRenderable::from))
                }
            }
        }
        // forward overlay rendering for base (content) offset by bannerHeight
        base.renderOverlay(context, x, y + bannerHeight + dependencyListHeight, width)
    }

    private fun findCurrentDependencyByLabel(label: String): FeatureDependencyResolver.Dependency? {
        currentRequirements.groups.forEach { g ->
            g.dependencies.forEach { d -> if (d.label == label) return d }
        }
        return null
    }

    /**
     * Expand given requirements recursively by resolving any dependencies-of-dependencies (both boolean fields and
     * third-party main toggles). Prevent cycles by tracking visited owners/thirdparties.
     */
    private fun expandRequirementsRecursive(root: FeatureDependencyResolver.Requirements): FeatureDependencyResolver.Requirements {
        val outGroups = mutableListOf<FeatureDependencyResolver.RequirementGroup>()
        val visitedFields = mutableSetOf<Pair<String, String>>() // ownerName#fieldName
        val visitedThird = mutableSetOf<String>()

        fun resolveFieldIfPresent(owner: Class<*>, fieldName: String) {
            val key = Pair(owner.name, fieldName)
            if (key in visitedFields) return
            visitedFields.add(key)
            val javaField = runCatching { owner.getDeclaredField(fieldName) }.getOrNull() ?: return
            val sub = FeatureDependencyResolver.resolve(javaField)
            if (sub.groups.isNotEmpty()) {
                outGroups.addAll(sub.groups)
                // recurse into sub dependencies
                sub.groups.forEach { g ->
                    g.dependencies.forEach { sd ->
                        when (val s = sd.source) {
                            is FeatureDependencyResolver.DependencySource.BooleanField -> resolveFieldIfPresent(s.owner, s.fieldName)
                        }
                    }
                }
            }
        }

        // Start by adding original groups, then expand
        outGroups.addAll(root.groups)
        root.groups.forEach { g ->
            g.dependencies.forEach { dep ->
                when (val s = dep.source) {
                    is FeatureDependencyResolver.DependencySource.BooleanField -> resolveFieldIfPresent(s.owner, s.fieldName)
                }
            }
        }

        // Remove duplicate dependencies (including the main toggle showing up multiple times
        // via recursive expansion) and duplicate groups
        val seenGroups = mutableSetOf<String>()
        val finalGroups = outGroups.mapNotNull { group ->
            val deduped = group.dependencies.distinctBy { keyOf(it) }
            val key = group.requireAll.toString() + "|" + group.message + "|" + deduped.joinToString(",") { keyOf(it) }
            if (!seenGroups.add(key)) null else group.copy(dependencies = deduped)
        }
        return FeatureDependencyResolver.Requirements(finalGroups)
    }

    private fun keyOf(dep: FeatureDependencyResolver.Dependency): String = when (val source = dep.source) {
        is FeatureDependencyResolver.DependencySource.BooleanField -> "${source.owner.name}#${source.fieldName}"
    }

    companion object {
        private const val MIN_BANNER_HEIGHT = 16

        /** Fixed vertical padding of the requirements banner, independent of the base editor height. */
        private const val BANNER_PADDING = 3
        private const val BLOCKED_BG = 0x33FF8888
        private const val SATISFIED_BG = 0x3323A55A
        private const val TEXT_COLOR = -0x1
        private const val OVERLAY_BG = 0x55000000
        private const val ROW_BG = 0x22000000
        private const val BUTTON_BG = 0xFF2E7D32.toInt()
        private const val BUTTON_BORDER = 0xFF1B5E20.toInt()
        private const val BUTTON_HIGHLIGHT = 0xFF66BB6A.toInt()
        private const val BUTTON_HOVER_OVERLAY = 0x44333333

        /** Maximum height of the red "blocked" overlay drawn over a tall editor. */
        private const val BLOCKED_OVERLAY_MAX_HEIGHT = 30

        // Debugging: when true, draw hitboxes for enable buttons
        private const val DEBUG_HITBOX = false
    }
}
