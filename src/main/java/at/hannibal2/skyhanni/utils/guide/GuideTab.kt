package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import net.minecraft.client.gui.Gui
import net.minecraft.item.ItemStack

class GuideTab(
    val item: ItemStack,
    val tip: Renderable,
    val isVertical: Boolean = false,
    var lastTab: GuideGUI.tabWrapper,
    val onClick: (GuideTab) -> Unit
) {

    fun fakeClick() = click()

    private fun click() {
        onClick.invoke(this)
        this.select()
        if (lastTab.tab != this) {
            lastTab.tab?.unSelect()
            lastTab.tab = this
        }
    }

    fun select() {
        selectColor = selectedColor
    }

    fun unSelect() {
        selectColor = notSelectedColor
    }

    fun isSelected() = selectColor == selectedColor

    val width = if (isVertical) tabLongSide else tabShortSide
    val height = if (isVertical) tabShortSide else tabLongSide

    private var selectColor = notSelectedColor

    private val renderable = Renderable.clickAndHover(object : Renderable {
        override val width = this@GuideTab.width
        override val height = this@GuideTab.height
        override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT
        override val verticalAlign: VerticalAlignment = VerticalAlignment.TOP

        val itemRender = Renderable.itemStack(
            item, 1.0, horizontalAlign = HorizontalAlignment.CENTER, verticalAlign = VerticalAlignment.CENTER
        )

        override fun render(posX: Int, posY: Int) {
            Gui.drawRect(0, 0, width, height, selectColor)
            itemRender.renderXYAligned(posX, posY, width, height)
        }
    }, listOf(tip), onClick = {
        click()
        SoundUtils.playClickSound()
    })

    fun render(posX: Int, posY: Int) {
        renderable.render(posX, posY)
    }
}
