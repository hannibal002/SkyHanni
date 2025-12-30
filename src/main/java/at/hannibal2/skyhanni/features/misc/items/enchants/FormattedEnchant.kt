package at.hannibal2.skyhanni.features.misc.items.enchants

import at.hannibal2.skyhanni.utils.NumberUtil.toRoman
import at.hannibal2.skyhanni.utils.compat.append
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

data class FormattedEnchant(
    private val enchant: Enchant,
    private val level: Int,
    private val stacking: String,
    private val isRoman: Boolean,
) : Comparable<FormattedEnchant> {
    private val loreDescription: MutableList<Component> = mutableListOf()

    fun addLore(lineOfLore: Component) = loreDescription.add(lineOfLore)

    fun getLore() = loreDescription

    override fun compareTo(other: FormattedEnchant) = this.enchant.compareTo(other.enchant)

    fun getComponent(itemStack: ItemStack?): Component {
        return enchant.getComponent(level, itemStack).append(if (isRoman) " ${level.toRoman()}" else " $level")
    }
}
