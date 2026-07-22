package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag

fun SafeItemStack.getTooltip(advanced: Boolean = false): MutableList<Component> {
    val tooltipType = if (advanced) TooltipFlag.ADVANCED else TooltipFlag.NORMAL
    return this.getTooltipLines(Item.TooltipContext.EMPTY, Minecraft.getInstance().player, tooltipType)
}

fun SafeItemStack.getTooltipCompat(advanced: Boolean = false): MutableList<String> {
    val tooltipType = if (advanced) TooltipFlag.ADVANCED else TooltipFlag.NORMAL
    return this.getTooltipLines(Item.TooltipContext.EMPTY, Minecraft.getInstance().player, tooltipType).map { it.formattedTextCompat() }
        .toMutableList()
}

fun Item.getIdentifierString(): String {
    return BuiltInRegistries.ITEM.getKey(this).toString()
}

/*
 * On modern, it will return Items.AIR if it can't find it instead of null
 */
fun String.getVanillaItem(): Item? {
    val item = BuiltInRegistries.ITEM.getValue(Identifier.parse(this))
    if (item == Items.AIR) return null
    return item
}

fun SafeItemStack.setCustomItemName(name: String): SafeItemStack {
    val comp = name.asComponent {
        italic = false
    }
    this.set(DataComponents.CUSTOM_NAME, comp)
    return this
}

fun SafeItemStack.setCustomItemName(name: Component): SafeItemStack {
    var comp = name
    if (!comp.style.isItalic) {
        comp = comp.copy().withStyle(comp.style.withItalic(false))
    }
    this.set(DataComponents.CUSTOM_NAME, comp)
    return this
}

enum class DyeCompat(
    private val dyeColor: Int,
    private val stackType: Item,
) {
    WHITE(
        15,
        Items.BONE_MEAL,
    ),
    ORANGE(
        14,
        Items.ORANGE_DYE,
    ),
    MAGENTA(
        13,
        Items.MAGENTA_DYE,
    ),
    LIGHT_BLUE(
        12,
        Items.LIGHT_BLUE_DYE,
    ),
    YELLOW(
        11,
        Items.YELLOW_DYE,
    ),
    LIME(
        10,
        Items.LIME_DYE,
    ),
    PINK(
        9,
        Items.PINK_DYE,
    ),
    GRAY(
        8,
        Items.GRAY_DYE,
    ),
    LIGHT_GRAY(
        7,
        Items.LIGHT_GRAY_DYE,
    ),
    CYAN(
        6,
        Items.CYAN_DYE,
    ),
    PURPLE(
        5,
        Items.PURPLE_DYE,
    ),
    BLUE(
        4,
        Items.LAPIS_LAZULI,
    ),
    BROWN(
        3,
        Items.COCOA_BEANS,
    ),
    GREEN(
        2,
        Items.GREEN_DYE,
    ),
    RED(
        1,
        Items.RED_DYE,
    ),
    BLACK(
        0,
        Items.BLACK_DYE,
    )
    ;

    fun createStack(size: Int = 1) = SafeItemStack(stackType, size)

    companion object {

        fun SafeItemStack.isDye(dye: DyeCompat): Boolean = isDye(dye.dyeColor)

        /**
         * Check if the item is a dye.
         * Enter a metadata to check for a specific dye color.
         */
        fun SafeItemStack.isDye(metadata: Int = -1): Boolean {
            if (metadata == -1) {
                return entries.any { this.`is`(it.stackType) }
            }

            return this.`is`(fromDyeColor(metadata).stackType)
        }

        private fun fromDyeColor(dyeColor: Int): DyeCompat = entries.firstOrNull { it.dyeColor == dyeColor } ?: GRAY

        fun toDamage(stack: SafeItemStack): Int {
            return entries.firstOrNull { stack.`is`(it.stackType) }?.dyeColor ?: 0
        }

        fun createDyeStack(dyeColor: Int, size: Int = 1): SafeItemStack =
            fromDyeColor(dyeColor).createStack(size)
    }
}
