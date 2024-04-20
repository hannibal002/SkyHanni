package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SoundUtils
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

    private fun onClick(): () -> Unit = when (this) {
        in armor -> {
            {
                SoundUtils.playClickSound()
                currentArmor = if (selectedState) null else this
                armor.forEach {
                    it.selectedState = it == currentArmor
                }
            }
        }

        in equip -> {
            {
                SoundUtils.playClickSound()
                currentEquip = if (selectedState) null else this
                equip.forEach {
                    it.selectedState = it == currentEquip
                }
            }
        }

        in pets -> {
            {
                val prev = currentPet
                currentPet = if (selectedState) lastEquippedPet else this
                if (prev != currentPet) {
                    SoundUtils.playClickSound()
                }
                pets.forEach {
                    it.selectedState = it == currentPet
                }
                FFStats.getTotalFF()
            }
        }

        else -> {
            {}
        }
    }

    fun getDisplay(clickEnabled: Boolean = false) = object : Renderable {

        val content = Renderable.clickable(
            Renderable.itemStackWithTip(
                getItem(), 1.0, 0, 0, false
            ),
            onClick = onClick(),
            condition = { clickEnabled })

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

        var lastEquippedPet = ELEPHANT

        var currentPet: FarmingItems = lastEquippedPet
        var currentArmor: FarmingItems? = null
        var currentEquip: FarmingItems? = null

        val armor = listOf(HELMET, CHESTPLATE, LEGGINGS, BOOTS)
        val equip = listOf(NECKLACE, CLOAK, BELT, BRACELET)
        val pets = listOf(ELEPHANT, MOOSHROOM_COW, RABBIT, BEE)

        fun getArmorDisplay(clickEnabled: Boolean = false): List<Renderable> = armor.map { it.getDisplay(clickEnabled) }

        fun getEquipmentDisplay(clickEnabled: Boolean = false): List<Renderable> =
            equip.map { it.getDisplay(clickEnabled) }

        fun getPetsDisplay(clickEnabled: Boolean = false): List<Renderable> = pets.map { it.getDisplay(clickEnabled) }
        fun resetClickState() {
            entries.filterNot { pets.contains(it) }.forEach { it.selectedState = false }
        }

        fun setDefaultPet(): FarmingItems {
            currentPet = lastEquippedPet
            pets.forEach {
                it.selectedState = it == currentPet
            }
            return lastEquippedPet
        }
    }
}
