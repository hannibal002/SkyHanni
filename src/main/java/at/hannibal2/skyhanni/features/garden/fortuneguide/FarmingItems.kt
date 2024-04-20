package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.gui.GuiScreen

enum class FarmingItems {
    WHEAT,
    CARROT,
    POTATO,
    NETHER_WART,
    PUMPKIN,
    MELON,
    COCOA_BEANS,
    SUGAR_CANE,
    CACTUS,
    MUSHROOM,
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS,
    NECKLACE,
    CLOAK,
    BELT,
    BRACELET,
    ELEPHANT,
    MOOSHROOM_COW,
    RABBIT,
    BEE,
    ;

    var selectedState = false

    fun getItem() = getItemOrNull() ?: FFGuideGUI.getFallbackItem(this)

    fun getItemOrNull() = ProfileStorageData.profileSpecific?.garden?.fortune?.farmingItems?.get(this)

    fun getDisplay() = object : Renderable {

        val content = Renderable.clickable(
            Renderable.itemStackWithTip(
                getItem(), 1.0, 0, 0, false
            ),
            onClick = {},
            condition = { !selectedState })

        override val width = content.width
        override val height = content.height
        override val horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
        override val verticalAlign = RenderUtils.VerticalAlignment.CENTER

        override fun render(posX: Int, posY: Int) {
            GuiScreen.drawRect(
                0,
                0,
                width,
                height,
                if (this@FarmingItems.selectedState) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
            )
            content.render(posX, posY)
        }
    }

    companion object {

        fun getArmorDisplay(): List<Renderable> = listOf(HELMET, CHESTPLATE, LEGGINGS, BOOTS).map { it.getDisplay() }

        fun getEquipmentDisplay(): List<Renderable> = listOf(NECKLACE, CLOAK, BELT, BRACELET).map { it.getDisplay() }

        fun getPetsDisplay(): List<Renderable> = listOf(ELEPHANT, MOOSHROOM_COW, RABBIT, BEE).map { it.getDisplay() }
    }
}
