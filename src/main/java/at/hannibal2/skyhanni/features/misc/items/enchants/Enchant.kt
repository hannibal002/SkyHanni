package at.hannibal2.skyhanni.features.misc.items.enchants

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.chroma.ChromaManager
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.extraAttributes
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.StringUtils.insert
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.splitCamelCase
import at.hannibal2.skyhanni.utils.compat.getDoubleOrDefault
import at.hannibal2.skyhanni.utils.compat.withColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.observer.Property
import java.util.TreeSet
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.world.item.ItemStack

open class Enchant : Comparable<Enchant> {

    // TODO move this away. split json data from logic
    @Expose
    var nbtName = ""

    @Expose
    var loreName = ""

    @Expose
    private val goodLevel = 0

    @Expose
    private val maxLevel = 0

    private fun isNormal() = this is Normal
    private fun isUltimate() = this is Ultimate
    private fun isStacking() = this is Stacking

    val config by lazy { SkyHanniMod.feature.inventory.enchantParsing }
    val advanced by lazy { config.advancedEnchantColors }

    open fun getComponent(level: Int, itemStack: ItemStack?): Component {
        return Component.literal(loreName).setStyle(getStyle(level, itemStack))
    }

    open fun getStyle(level: Int, itemStack: ItemStack? = null): Style {
        val colorProperty: Property<out Any> = when {
            level >= maxLevel -> if (advanced.useAdvancedPerfectColor.get()) advanced.advancedPerfectColor else config.perfectEnchantColor
            level > goodLevel -> if (advanced.useAdvancedGreatColor.get()) advanced.advancedGreatColor else config.greatEnchantColor
            level == goodLevel -> if (advanced.useAdvancedGoodColor.get()) advanced.advancedGoodColor else config.goodEnchantColor
            else -> if (advanced.useAdvancedPoorColor.get()) advanced.advancedPoorColor else config.poorEnchantColor
        }

        // Exceptions
        checkExceptions(level, itemStack)?.let { return it }

        if (colorProperty.get() is LorenzColor &&
            colorProperty.get() == LorenzColor.CHROMA && // If enchant color is chroma
            !(ChromaManager.config.enabled.get() || EnchantParser.isSbaLoaded)) { // and chroma is disabled
            return Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true) // return bold gold color
        }

        var style = if (colorProperty.get() is LorenzColor) {
            if (colorProperty.get() == LorenzColor.CHROMA)
                Style.EMPTY.withColor(TextColor(0xFFFFFF, "chroma"))
            else
                Style.EMPTY.withColor(TextColor.fromRgb((colorProperty.get() as LorenzColor).toColor().rgb))
        } else {
            Style.EMPTY.withColor((colorProperty.get() as ChromaColour).getEffectiveColourRGB())
        }

        if (level >= maxLevel && config.boldPerfectEnchant.get()) style = style.withBold(true)

        return style
    }

    /**
     * Method to check for certain or unique exceptions that need to be handled explicitly.
     *
     * *(There isn't much of a convention to adding exceptions, except try to include relevant exceptions under
     * a corresponding enchantment conditional, unless the exception is not specific to a certain enchant. i.e.
     * Efficiency exceptions should be within the `if (this.nbtName == "efficiency")` conditional)*
     *
     * @param level The level of the enchant currently being parsed
     * @param itemStack The ItemStack of the hovered item. Can be null, e.g. when hovering over `/show` items
     */
    private fun checkExceptions(level: Int, itemStack: ItemStack?): Style? {
        val itemCategory = itemStack?.getItemCategoryOrNull()
        val internalName = itemStack?.getInternalNameOrNull()
        val itemName = internalName?.repoItemName?.removeColor()

        if (this.nbtName == "efficiency") {
            // If the item is a Stonk, or a non-mining tool with Efficiency 5 (whilst not being a Promising Shovel),
            // color the enchant as max
            if (itemName == "Stonk" ||
                (itemCategory != null && !ItemCategory.miningTools.contains(itemCategory) && level == 5 && itemName != "Promising Shovel")
            ) {
                return if (advanced.useAdvancedPerfectColor.get()) {
                    Style.EMPTY.withColor(advanced.advancedPerfectColor.get().getEffectiveColourRGB())
                } else {
                    if (config.perfectEnchantColor.get() == LorenzColor.CHROMA)
                        Style.EMPTY.withColor(TextColor(0xFFFFFF, "chroma"))
                    else
                        Style.EMPTY.withColor(config.perfectEnchantColor.get().toChromaColor().getEffectiveColourRGB())
                }
            }
        }

        return null
    }

    override fun toString() = "$nbtName $goodLevel $maxLevel\n"

    override fun compareTo(other: Enchant): Int {
        if (this.isUltimate() == other.isUltimate()) {
            if (this.isStacking() == other.isStacking()) {
                return this.loreName.compareTo(other.loreName)
            }
            return if (this.isStacking()) -1 else 1
        }
        return if (this.isUltimate()) -1 else 1
    }

    class Normal : Enchant()

    class Ultimate : Enchant() {
        override fun getStyle(level: Int, itemStack: ItemStack?): Style {
            return if (advanced.useAdvancedUltimateColor.get()) {
                Style.EMPTY.withColor(advanced.advancedUltimateColor.get().getEffectiveColourRGB()).withBold(true)
            } else {
                if (config.ultimateEnchantColor.get() == LorenzColor.CHROMA)
                    Style.EMPTY.withColor(TextColor(0xFFFFFF, "chroma")).withBold(true)
                else
                    Style.EMPTY.withColor(config.ultimateEnchantColor.get().toColor().rgb).withBold(true)
            }
        }
    }

    class Stacking : Enchant() {
        @Expose
        private val nbtNum: String? = null

        @Expose
        @Suppress("UnusedPrivateProperty")
        private val statLabel: String? = null

        @Expose
        private val stackLevel: TreeSet<Int>? = null

        override fun toString() = "$nbtNum $stackLevel ${super.toString()}"

        fun progressString(item: ItemStack): String {
            val nbtKey = nbtNum ?: return ""
            val levels = stackLevel ?: return ""
            val label = statLabel?.splitCamelCase()?.replaceFirstChar { it.uppercase() }?.replace("Xp", "XP") ?: return ""
            val progress = item.extraAttributes.getDoubleOrDefault(nbtKey).roundTo(0).toInt()
            if (progress == 0) return ""
            val nextLevel = levels.higher(progress)
            val tail = nextLevel?.shortFormat()?.insert(0, "/ ") ?: "(Maxed)"
            return "§7$label: §c${progress.shortFormat()} §7$tail"
        }
    }

    class Dummy(name: String) : Enchant() {
        init {
            loreName = name
            nbtName = name
        }

        // Ensures enchants not yet in repo stay as vanilla formatting
        // (instead of that stupid dark red lowercase formatting *cough* sba *cough*)
        override fun getComponent(level: Int, itemStack: ItemStack?): Component {
            return Component.literal(loreName).withColor(ChatFormatting.BLUE)
        }
    }
}
