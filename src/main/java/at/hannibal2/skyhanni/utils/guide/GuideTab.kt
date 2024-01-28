package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.HorizontalAlignment
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.VerticalAlignment
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack

class GuideTab(val item: ItemStack, val tip: Renderable, val isVertical: Boolean = false, var lastTab: GuideGUI.tabWrapper, val onClick: (GuideTab) -> Unit) {

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

    private val renderable = Renderable.clickAndHover(
        object : Renderable {
            override val width = this@GuideTab.width
            override val height = this@GuideTab.height
            override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.Left
            override val verticalAlign: VerticalAlignment = VerticalAlignment.Top

            val itemRender = Renderable.itemStack(item, 5.0 / 3.0)

            override fun render(posX: Int, posY: Int) {
                Gui.drawRect(0, 0, width, height, selectColor)
                GlStateManager.translate(tabPadding, tabPadding, 0f)
                itemRender.render(posX + tabPadding.toInt(), posY + tabPadding.toInt())
                GlStateManager.translate(-tabPadding, -tabPadding, 0f)
            }
        },
        listOf(tip),
        onClick = {
            click()
            SoundUtils.playClickSound()
        })

    fun render(posX: Int, posY: Int) {
        renderable.render(posX, posY)
    }
}
