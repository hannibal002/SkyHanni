package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.LEFT_MOUSE
import at.hannibal2.skyhanni.utils.KeyboardManager.RIGHT_MOUSE
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.putAt
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.clickable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.clickableAndScrollable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.hoverTips
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRenderable
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
//#if MC > 1.21
//$$ import net.minecraft.text.Text
//#endif

@Suppress("TooManyFunctions", "unused", "MemberVisibilityCanBePrivate")
internal object RenderableUtils {

    /** Calculates the relative x position of the columns in a table*/
    fun calculateTableX(content: Collection<List<Renderable?>>, xPadding: Int): List<Int> {
        var index = 0
        return buildList {
            while (true) {
                val x = content.map { it.getOrNull(index) }.takeIf { it.any { it != null } }?.maxOfOrNull {
                    it?.width ?: 0
                }?.let { it + xPadding } ?: break
                add(x)
                index++
            }
        }
    }

    /** Calculates the relative y position of the rows in a table*/
    fun calculateTableY(content: Collection<List<Renderable?>>, yPadding: Int): Map<List<Renderable?>, Int> {
        return content.associateWith { row ->
            (row.maxOfOrNull { it?.height ?: 0 } ?: 0) + yPadding
        }
    }

    /** Calculates the absolute x position of the columns in a table*/
    fun calculateTableXOffsets(content: Collection<Collection<Renderable?>>, xPadding: Int) = run {
        val rows: List<List<Renderable?>> = content.map { it.toList() }
        var buffer = 0
        var index = 0
        buildList {
            add(0)
            while (true) {
                buffer += rows.map { it.getOrNull(index) }.takeIf { it.any { it != null } }?.maxOfOrNull {
                    it?.width ?: 0
                }?.let { it + xPadding } ?: break
                add(buffer)
                index++
            }
            if (this.size == 1) {
                add(xPadding)
            }
        }
    }

    /** Calculates the absolute y position of the rows in a table*/
    fun calculateTableYOffsets(content: Collection<Collection<Renderable?>>, yPadding: Int) = run {
        var buffer = 0
        listOf(0) + (
            content.takeIf { it.isNotEmpty() }?.map { row ->
                buffer += (row.maxOfOrNull { it?.height ?: 0 } ?: 0) + yPadding
                buffer
            } ?: listOf(yPadding)
            )
    }

    fun calculateAlignmentXOffset(width: Int, xSpace: Int, alignment: HorizontalAlignment) = when (alignment) {
        HorizontalAlignment.CENTER -> (xSpace - width) / 2
        HorizontalAlignment.RIGHT -> xSpace - width
        else -> 0
    }

    fun calculateAlignmentYOffset(height: Int, ySpace: Int, alignment: VerticalAlignment) = when (alignment) {
        VerticalAlignment.CENTER -> (ySpace - height) / 2
        VerticalAlignment.BOTTOM -> ySpace - height
        else -> 0
    }

    private fun calculateAlignmentXOffset(renderable: Renderable, xSpace: Int) = when (renderable.horizontalAlign) {
        HorizontalAlignment.LEFT -> 0
        HorizontalAlignment.CENTER -> (xSpace - renderable.width) / 2
        HorizontalAlignment.RIGHT -> xSpace - renderable.width
        else -> 0
    }

    private fun calculateAlignmentYOffset(renderable: Renderable, ySpace: Int) = when (renderable.verticalAlign) {
        VerticalAlignment.TOP -> 0
        VerticalAlignment.CENTER -> (ySpace - renderable.height) / 2
        VerticalAlignment.BOTTOM -> ySpace - renderable.height
        else -> 0
    }

    fun Renderable.renderAndScale(mouseOffsetX: Int, mouseOffsetY: Int, xSpace: Int, ySpace: Int, padding: Int = 5) {
        val xWithoutPadding = xSpace - padding * 2
        val yWithoutPadding = ySpace - padding * 2

        val xScale = xWithoutPadding / width.toFloat()
        val yScale = yWithoutPadding / height.toFloat()
        val scale = min(xScale, yScale)
        val inverseScale = 1 / scale

        val subWidth = ceil(width * scale).toInt()
        val subHeight = ceil(height * scale).toInt()

        val xOffset = calculateAlignmentXOffset(subWidth, xWithoutPadding, horizontalAlign)
        val yOffset = calculateAlignmentYOffset(subHeight, yWithoutPadding, verticalAlign)

        val xOffsetRender = (xOffset + padding).toFloat()
        val yOffsetRender = (yOffset + padding).toFloat()

        val preScaleMouse = Renderable.currentRenderPassMousePosition ?: (0 to 0)
        try {
            Renderable.currentRenderPassMousePosition =
                ((preScaleMouse.first - padding) * inverseScale).toInt() to ((preScaleMouse.second - padding) * inverseScale).toInt()

            DrawContextUtils.translate(xOffsetRender, yOffsetRender, 0f)
            DrawContextUtils.scale(scale, scale, 1f)
            render(
                mouseOffsetX + (xOffset * inverseScale).toInt(),
                mouseOffsetY + (yOffset * inverseScale).toInt(),
            )
            DrawContextUtils.scale(inverseScale, inverseScale, 1f)
            DrawContextUtils.translate(-xOffsetRender, -yOffsetRender, 0f)
        } finally {
            Renderable.currentRenderPassMousePosition = preScaleMouse
        }
    }

    fun Renderable.renderXYAligned(mouseOffsetX: Int, mouseOffsetY: Int, xSpace: Int, ySpace: Int): Pair<Int, Int> {
        val xOffset = calculateAlignmentXOffset(this, xSpace)
        val yOffset = calculateAlignmentYOffset(this, ySpace)
        DrawContextUtils.translate(xOffset.toFloat(), yOffset.toFloat(), 0f)
        this.render(mouseOffsetX + xOffset, mouseOffsetY + yOffset)
        DrawContextUtils.translate(-xOffset.toFloat(), -yOffset.toFloat(), 0f)
        return xOffset to yOffset
    }

    fun Renderable.renderXAligned(mouseOffsetX: Int, mouseOffsetY: Int, xSpace: Int): Int {
        val xOffset = calculateAlignmentXOffset(this, xSpace)
        DrawContextUtils.translate(xOffset.toFloat(), 0f, 0f)
        this.render(mouseOffsetX + xOffset, mouseOffsetY)
        DrawContextUtils.translate(-xOffset.toFloat(), 0f, 0f)
        return xOffset
    }

    fun Renderable.renderYAligned(mouseOffsetX: Int, mouseOffsetY: Int, ySpace: Int): Int {
        val yOffset = calculateAlignmentYOffset(this, ySpace)
        DrawContextUtils.translate(0f, yOffset.toFloat(), 0f)
        this.render(mouseOffsetX, mouseOffsetY + yOffset)
        DrawContextUtils.translate(0f, -yOffset.toFloat(), 0f)
        return yOffset
    }

    fun renderString(
        text: String,
        scale: Double = 1.0,
        color: Color = Color.WHITE,
        inverseScale: Double = 1 / scale,
    ) {
        DrawContextUtils.translate(1.0, 1.0, 0.0)
        DrawContextUtils.scale(scale.toFloat(), scale.toFloat(), 1f)
        GuiRenderUtils.drawString(text, 0f, 0f, color.rgb)
        DrawContextUtils.scale(inverseScale.toFloat(), inverseScale.toFloat(), 1f)
        DrawContextUtils.translate(-1.0, -1.0, 0.0)
    }

    //#if MC > 1.21
    //$$ fun renderString(
    //$$     text: Text,
    //$$     scale: Double = 1.0,
    //$$     color: Color = Color.WHITE,
    //$$     inverseScale: Double = 1 / scale,
    //$$ ) {
    //$$     DrawContextUtils.translate(1.0, 1.0, 0.0)
    //$$     DrawContextUtils.scale(scale.toFloat(), scale.toFloat(), 1f)
    //$$     GuiRenderUtils.drawString(text, 0f, 0f, color.rgb)
    //$$     DrawContextUtils.scale(inverseScale.toFloat(), inverseScale.toFloat(), 1f)
    //$$     DrawContextUtils.translate(-1.0, -1.0, 0.0)
    //$$ }
    //#endif

    inline fun <T> MutableList<Searchable>.addNullableButton(
        label: String,
        current: T?,
        crossinline onChange: (T?) -> Unit,
        universe: List<T?>,
        nullLabel: String,
        enableUniverseScroll: Boolean = true,
    ) {
        val map = universe.associateWithTo(LinkedHashMap()) { it.toString() }
        map.putAt(0, null, nullLabel)

        val currentName = map[current] ?: error("unknown entry $current in map")
        add(
            createButtonNew(
                label = label,
                current = currentName,
                getName = { it ?: nullLabel },
                onChange = { newString ->
                    val newKey = map.entries.first { it.value == newString }.key
                    onChange(newKey)
                },
                universe = map.values.toList(),
                enableUniverseScroll = enableUniverseScroll,
            ),
        )
    }

    inline fun MutableList<Searchable>.addButton(
        label: String,
        enabled: String,
        disabled: String,
        config: KMutableProperty0<Boolean>,
        crossinline onChange: () -> Unit,
        enableUniverseScroll: Boolean = true,
        scrollValue: ScrollValue = ScrollValue(),
    ) {
        add(createBooleanButton(label, enabled, disabled, config, onChange, enableUniverseScroll, scrollValue))
    }

    inline fun <T> MutableList<Searchable>.addButton(
        label: String,
        current: T,
        crossinline getName: (T) -> String,
        crossinline onChange: (T) -> Unit,
        universe: List<T>,
        enableUniverseScroll: Boolean = true,
    ) {
        add(
            createButtonNew(
                label,
                current,
                getName = { getName(it ?: error("it is null in non-nullable getName()")) },
                onChange = { onChange(it ?: error("it is null in non-nullable onChange()")) },
                universe,
                enableUniverseScroll,
            ),
        )
    }

    inline fun MutableList<Renderable>.addRenderableButton(
        label: String,
        enabled: String,
        disabled: String,
        config: KMutableProperty0<Boolean>,
        crossinline onChange: () -> Unit,
        enableUniverseScroll: Boolean = true,
        scrollValue: ScrollValue = ScrollValue(),
    ) {
        add(createBooleanButton(label, enabled, disabled, config, onChange, enableUniverseScroll, scrollValue).renderable)
    }

    private inline fun createBooleanButton(
        label: String,
        enabled: String,
        disabled: String,
        config: KMutableProperty0<Boolean>,
        crossinline onChange: () -> Unit,
        enableUniverseScroll: Boolean,
        scrollValue: ScrollValue,
    ): Searchable {

        val current = config.get()
        val element = createButtonNew(
            label,
            current = if (current) enabled else disabled,
            getName = { it ?: error("it is null in non-nullable getName()") },
            onChange = {
                config.set(!current)
                onChange()
            },
            universe = listOf(enabled, disabled),
            enableUniverseScroll,
            scrollValue,
        )
        return element
    }

    inline fun <reified T : Enum<T>> MutableList<Renderable>.addRenderableButton(
        label: String,
        current: T?,
        crossinline getName: (T) -> String = { it.toString() },
        crossinline onChange: (T) -> Unit,
        universe: List<T> = enumValues<T>().toList(),
        enableUniverseScroll: Boolean = true,
        scrollValue: ScrollValue = ScrollValue(),
    ) {
        add(
            createButtonNew(
                label,
                current,
                getName = { getName(it ?: error("it is null in non-nullable getName()")) },
                onChange = { onChange(it ?: error("it is null in non-nullable onChange()")) },
                universe,
                enableUniverseScroll,
                scrollValue,
            ).renderable,
        )
    }

    inline fun <reified T : Enum<T>> MutableList<Renderable>.addRenderableNullableButton(
        label: String,
        current: T?,
        crossinline getName: (T?) -> String = { it?.toString().orEmpty() },
        crossinline onChange: (T?) -> Unit,
        universe: List<T?> = enumValues<T>().toList(),
        enableUniverseScroll: Boolean = true,
    ) {
        add(createButtonNew(label, current, getName, onChange, universe, enableUniverseScroll).renderable)
    }

    fun <T> List<T?>.circle(current: T?): T? {
        val index = indexOf(current)
        val newIndex = (index + 1) % size // Increment index and wrap around
        return get(newIndex)
    }

    fun <T> List<T?>.circleBackwards(current: T?): T? {
        val index = indexOf(current)
        val newIndex = ((index - 1 + size)) % size // Increment index and wrap around
        return get(newIndex)
    }

    private inline fun <T> createButtonNew(
        label: String,
        current: T?,
        crossinline getName: (T?) -> String,
        crossinline onChange: (T?) -> Unit,
        universe: List<T?>,
        enableUniverseScroll: Boolean = true,
        scrollValue: ScrollValue = ScrollValue(),
    ): Searchable {
        val currentName = getName(current)
        val tips = buildList {
            add("§a$label")
            add(" ")
            for (entry in universe) {
                val name = getName(entry)
                if (entry == current) {
                    this.add("§e▶ $name")
                } else {
                    this.add("§7  $name")
                }
            }
            add(" ")
            add("§bRight-click to go backwards!")
            add("§eClick to switch $label!")
            if (enableUniverseScroll) {
                add("§8You can also mouse scroll!")
            }
        }

        val onClick: (Int) -> Unit = onClick@{ keyCode ->
            if ((System.currentTimeMillis() - ChatUtils.lastButtonClicked) < 150) return@onClick
            val next = when (keyCode) {
                LEFT_MOUSE -> universe.circle(current)
                RIGHT_MOUSE -> universe.circleBackwards(current)
                else -> return@onClick
            }
            onChange(next)
            SoundUtils.playClickSound()
            ChatUtils.lastButtonClicked = System.currentTimeMillis()
        }

        val clickMap = mapOf(
            LEFT_MOUSE to { onClick(LEFT_MOUSE) },
            RIGHT_MOUSE to { onClick(RIGHT_MOUSE) },
        )

        return Renderable.line {
            addString("§7$label §a[")
            val displayFormat = hoverTips("§e$currentName", tips, bypassChecks = false, onHover = {})
            when (enableUniverseScroll) {
                true -> clickableAndScrollable(displayFormat, onAnyClick = clickMap, bypassChecks = false, scrollValue = scrollValue)
                false -> clickable(displayFormat, onAnyClick = clickMap, bypassChecks = false)
            }.let { add(it) }
            addString("§a]")
        }.toSearchable()
    }

    fun MutableList<Renderable>.addCenteredString(string: String) =
        this.add(StringRenderable(string, horizontalAlign = HorizontalAlignment.CENTER))

    fun fillTable(
        data: List<DisplayTableEntry>,
        padding: Int = 1,
        itemScale: Double = NeuItems.ITEM_FONT_SIZE,
    ): Renderable {
        val outerList = constructOuterList(data, itemScale)
        return Renderable.table(outerList, xPadding = 5, yPadding = padding)
    }

    fun fillScrollTable(
        data: List<DisplayTableEntry>,
        padding: Int = 1,
        itemScale: Double = NeuItems.ITEM_FONT_SIZE,
        height: Int,
        velocity: Double = 2.0,
    ): Renderable {
        val outerList = constructOuterList(data, itemScale)
        return Renderable.scrollTable(outerList, height, xPadding = 5, yPadding = padding, velocity = velocity)
    }

    private fun constructOuterList(
        data: List<DisplayTableEntry>,
        itemScale: Double = NeuItems.ITEM_FONT_SIZE,
    ): MutableList<List<Renderable>> {
        val sorted = data.sortedByDescending { it.sort }
        val outerList = mutableListOf<List<Renderable>>()
        for (entry in sorted) {
            val item = entry.item.getItemStackOrNull()?.let {
                ItemStackRenderable(it, scale = itemScale)
            } ?: continue
            val left = hoverTips(
                entry.left,
                tips = entry.hover,
                highlightsOnHoverSlots = entry.highlightsOnHoverSlots,
            )
            val right = StringRenderable(entry.right)
            outerList.add(listOf(item, left, right))
        }
        return outerList
    }
}

fun MutableList<Renderable>.addLine(builderAction: MutableList<Renderable>.() -> Unit) {
    add(HorizontalContainerRenderable(buildList { builderAction() }))
}

fun MutableList<Renderable>.addLine(tips: List<String>, builderAction: MutableList<Renderable>.() -> Unit) {
    add(hoverTips(HorizontalContainerRenderable(buildList { builderAction() }, 0), tips = tips))
}

internal abstract class RenderableWrapper internal constructor(protected val content: Renderable) : Renderable {
    override val width = content.width
    override val height = content.height
    override val horizontalAlign = content.horizontalAlign
    override val verticalAlign = content.verticalAlign
}
