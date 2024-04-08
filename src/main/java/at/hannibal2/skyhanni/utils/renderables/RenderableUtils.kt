package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.SoundUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack

internal object RenderableUtils {

    /** Calculates the absolute x position of the columns in a table*/
    fun calculateTableXOffsets(content: List<List<Renderable?>>, xPadding: Int) = run {
        var buffer = 0
        var index = 0
        buildList {
            add(0)
            while (true) {
                buffer += content.map { it.getOrNull(index) }.takeIf { it.any { it != null } }?.maxOf {
                    it?.width ?: 0
                }?.let { it + xPadding } ?: break
                add(buffer)
                index++
            }
        }
    }

    /** Calculates the absolute y position of the rows in a table*/
    fun calculateTableYOffsets(content: List<List<Renderable?>>, yPadding: Int) = run {
        var buffer = 0
        listOf(0) + content.map { row ->
            buffer += row.maxOf { it?.height ?: 0 } + yPadding
            buffer
        }
    }

    private fun calculateAlignmentXOffset(renderable: Renderable, xSpace: Int) = when (renderable.horizontalAlign) {
        HorizontalAlignment.LEFT -> 0
        HorizontalAlignment.CENTER -> (xSpace - renderable.width) / 2
        HorizontalAlignment.RIGHT -> xSpace - renderable.width
    }

    private fun calculateAlignmentYOffset(renderable: Renderable, ySpace: Int) = when (renderable.verticalAlign) {
        VerticalAlignment.TOP -> 0
        VerticalAlignment.CENTER -> (ySpace - renderable.height) / 2
        VerticalAlignment.BOTTOM -> ySpace - renderable.height
    }

    fun Renderable.renderXYAligned(posX: Int, posY: Int, xSpace: Int, ySpace: Int) {
        val xOffset = calculateAlignmentXOffset(this, xSpace)
        val yOffset = calculateAlignmentYOffset(this, ySpace)
        GlStateManager.translate(xOffset.toFloat(), yOffset.toFloat(), 0f)
        this.render(posX + xOffset, posY + yOffset)
        GlStateManager.translate(-xOffset.toFloat(), -yOffset.toFloat(), 0f)
    }

    fun Renderable.renderXAligned(posX: Int, posY: Int, xSpace: Int) {
        val xOffset = calculateAlignmentXOffset(this, xSpace)
        GlStateManager.translate(xOffset.toFloat(), 0f, 0f)
        this.render(posX + xOffset, posY)
        GlStateManager.translate(-xOffset.toFloat(), 0f, 0f)
    }

    fun Renderable.renderYAligned(posX: Int, posY: Int, ySpace: Int) {
        val yOffset = calculateAlignmentYOffset(this, ySpace)
        GlStateManager.translate(0f, yOffset.toFloat(), 0f)
        this.render(posX, posY + yOffset)
        GlStateManager.translate(0f, -yOffset.toFloat(), 0f)
    }

    // TODO add cache
    fun MutableList<Renderable>.addString(
        text: String,
        horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
        verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
    ) {
        add(Renderable.string(text, horizontalAlign = horizontalAlign, verticalAlign = verticalAlign))
    }

    // TODO add internal name support, and caching
    fun MutableList<Renderable>.addItemStack(itemStack: ItemStack) {
        add(Renderable.itemStack(itemStack))
    }

    fun MutableList<Renderable>.addItemStack(internalName: NEUInternalName) {
        addItemStack(internalName.getItemStack())
    }

    inline fun <reified T : Enum<T>> MutableList<Renderable>.addSelector(
        prefix: String,
        getName: (T) -> String,
        isCurrent: (T) -> Boolean,
        crossinline onChange: (T) -> Unit,
    ) {
        add(Renderable.horizontalContainer(buildSelector<T>(prefix, getName, isCurrent, onChange)))
    }

    inline fun <reified T : Enum<T>> buildSelector(
        prefix: String,
        getName: (T) -> String,
        isCurrent: (T) -> Boolean,
        crossinline onChange: (T) -> Unit,
    ) = buildList {
        addString(prefix)
        for (entry in enumValues<T>()) {
            val display = getName(entry)
            if (isCurrent(entry)) {
                addString("§a[$display]")
            } else {
                addString("§e[")
                add(Renderable.link("§e$display") {
                    onChange(entry)
                })
                addString("§e]")
            }
            addString(" ")
        }
    }

    inline fun MutableList<Renderable>.addButton(
        prefix: String,
        getName: String,
        crossinline onChange: () -> Unit,
        tips: List<String> = emptyList(),
    ) {
        val onClick = {
            if ((System.currentTimeMillis() - ChatUtils.lastButtonClicked) > 150) { // funny thing happen if I don't do that
                onChange()
                SoundUtils.playClickSound()
                ChatUtils.lastButtonClicked = System.currentTimeMillis()
            }
        }
        add(Renderable.horizontalContainer(buildList {
            addString(prefix)
            addString("§a[")
            if (tips.isEmpty()) {
                add(Renderable.link("§e$getName", false, onClick))
            } else {
                add(Renderable.clickAndHover("§e$getName", tips, false, onClick))
            }
            addString("§a]")
        }))
    }
}
