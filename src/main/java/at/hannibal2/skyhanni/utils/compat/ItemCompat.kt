package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.Minecraft
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

//#if MC > 1.16
//$$ import net.minecraft.world.item.DyeItem
//#endif
//#if MC > 1.21
//$$ import net.minecraft.item.tooltip.TooltipType
//$$ import net.minecraft.registry.Registries
//$$ import net.minecraft.util.Identifier
//$$ import net.minecraft.component.DataComponentTypes
//$$ import net.minecraft.text.Text
//#endif

fun ItemStack.getTooltipCompat(advanced: Boolean): MutableList<String> {
    //#if MC < 1.12
    return this.getTooltip(Minecraft.getMinecraft().thePlayer, advanced)
    //#elseif MC < 1.16
    //$$ return this.getTooltip(Minecraft.getMinecraft().player) { advanced }
    //#elseif MC < 1.21
    //$$ return this.getTooltipLines(Minecraft.getInstance().player) { advanced }.map { it.getFormattedTextCompat() }.toMutableList()
    //#else
    //$$ val tooltipType = if (advanced) TooltipType.ADVANCED else TooltipType.BASIC
    //$$ return this.getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, tooltipType).map { it.formattedTextCompat() }.toMutableList()
    //#endif
}

fun Item.getIdentifierString(): String {
    //#if MC < 1.16
    return this.registryName
    //#else
    //$$ return Registries.ITEM.getId(this).toString()
    //#endif
}

/*
 * On Modern it will return Items.AIR if it cant find it instead of null
 */
fun String.getVanillaItem(): Item? {
    //#if MC < 1.16
    return Item.getByNameOrId(this)
    //#else
    //$$ val item = Registries.ITEM.get(Identifier.of(this))
    //$$ if (item == Items.AIR) return null
    //$$ return item
    //#endif
}

fun ItemStack.setCustomItemName(name: String): ItemStack {
    //#if MC < 1.16
    this.setStackDisplayName(name)
    //#else
    //$$ this.set(DataComponentTypes.CUSTOM_NAME, Text.of(name))
    //#endif
    return this
}

//#if MC > 1.21
//$$    fun ItemStack.setCustomItemName(name: Text): ItemStack {
//$$     this.set(DataComponentTypes.CUSTOM_NAME, name)
//$$     return this
//$$ }
//#endif

enum class DyeCompat(
    private val dyeColor: Int,
    //#if MC > 1.16
    //$$ private val stackType: Item
    //#endif
) {
    WHITE(
        15,
        //#if MC > 1.16
        //$$ Items.WHITE_DYE
        //#endif
    ),
    ORANGE(
        14,
        //#if MC > 1.16
        //$$ Items.ORANGE_DYE
        //#endif
    ),
    MAGENTA(
        13,
        //#if MC > 1.16
        //$$ Items.MAGENTA_DYE
        //#endif
    ),
    LIGHT_BLUE(
        12,
        //#if MC > 1.16
        //$$ Items.LIGHT_BLUE_DYE
        //#endif
    ),
    YELLOW(
        11,
        //#if MC > 1.16
        //$$ Items.YELLOW_DYE
        //#endif
    ),
    LIME(
        10,
        //#if MC > 1.16
        //$$ Items.LIME_DYE
        //#endif
    ),
    PINK(
        9,
        //#if MC > 1.16
        //$$ Items.PINK_DYE
        //#endif
    ),
    GRAY(
        8,
        //#if MC > 1.16
        //$$ Items.GRAY_DYE
        //#endif
    ),
    LIGHT_GRAY(
        7,
        //#if MC > 1.16
        //$$ Items.LIGHT_GRAY_DYE
        //#endif
    ),
    CYAN(
        6,
        //#if MC > 1.16
        //$$ Items.CYAN_DYE
        //#endif
    ),
    PURPLE(
        5,
        //#if MC > 1.16
        //$$ Items.PURPLE_DYE
        //#endif
    ),
    BLUE(
        4,
        //#if MC > 1.16
        //$$ Items.BLUE_DYE
        //#endif
    ),
    BROWN(
        3,
        //#if MC > 1.16
        //$$ Items.BROWN_DYE
        //#endif
    ),
    GREEN(
        2,
        //#if MC > 1.16
        //$$ Items.GREEN_DYE
        //#endif
    ),
    RED(
        1,
        //#if MC > 1.16
        //$$ Items.RED_DYE
        //#endif
    ),
    BLACK(
        0,
        //#if MC > 1.16
        //$$ Items.BLACK_DYE
        //#endif
    )
    ;

    fun createStack(size: Int = 1): ItemStack =
        //#if MC < 1.16
        ItemStack(Items.dye, size, dyeColor)
    //#else
    //$$ ItemStack(stackType, size)
    //#endif

    companion object {

        fun ItemStack.isDye(dye: DyeCompat): Boolean = isDye(dye.dyeColor)

        /**
         * Check if the item is a dye.
         * Enter a metadata to check for a specific dye color.
         */
        fun ItemStack.isDye(metadata: Int = -1): Boolean {
            if (metadata == -1) {
                //#if MC < 1.16
                return this.item == Items.dye
                //#else
                //$$ return this.item is DyeItem
                //#endif
            }

            //#if MC < 1.16
            return this.item == Items.dye && this.metadata == metadata
            //#else
            //$$ return this.item == fromDyeColor(metadata).stackType
            //#endif
        }

        private fun fromDyeColor(dyeColor: Int): DyeCompat = entries.firstOrNull { it.dyeColor == dyeColor } ?: GRAY

        fun createDyeStack(dyeColor: Int, size: Int = 1): ItemStack =
            fromDyeColor(dyeColor).createStack(size)
    }
}
