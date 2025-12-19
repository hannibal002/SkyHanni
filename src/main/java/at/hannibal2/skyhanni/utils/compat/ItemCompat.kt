package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.Minecraft
import net.minecraft.world.item.Items
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
//#if MC > 1.16
import net.minecraft.world.item.DyeItem
//#endif
//#if MC > 1.21
import net.minecraft.world.item.TooltipFlag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
//#endif

fun ItemStack.getTooltipCompat(advanced: Boolean): MutableList<String> {
    //#if MC < 1.16
    //$$ return this.getTooltip(Minecraft.getMinecraft().thePlayer, advanced)
    //#elseif MC < 1.21
    //$$ return this.getTooltip(MinecraftClient.getInstance().player) { advanced }.map { it.getFormattedTextCompat() }.toMutableList()
    //#else
    val tooltipType = if (advanced) TooltipFlag.ADVANCED else TooltipFlag.NORMAL
    return this.getTooltipLines(Item.TooltipContext.EMPTY, Minecraft.getInstance().player, tooltipType).map { it.formattedTextCompat() }.toMutableList()
    //#endif
}

fun Item.getIdentifierString(): String {
    //#if MC < 1.16
    //$$ return this.registryName
    //#else
    return BuiltInRegistries.ITEM.getKey(this).toString()
    //#endif
}

/*
 * On Modern it will return Items.AIR if it cant find it instead of null
 */
fun String.getVanillaItem(): Item? {
    //#if MC < 1.16
    //$$ return Item.getByNameOrId(this)
    //#else
    val item = BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(this))
    if (item == Items.AIR) return null
    return item
    //#endif
}

fun ItemStack.setCustomItemName(name: String): ItemStack {
    //#if MC < 1.16
    //$$ this.setStackDisplayName(name)
    //#else
    this.set(DataComponents.CUSTOM_NAME, Component.nullToEmpty(name))
    //#endif
    return this
}

//#if MC > 1.21
   fun ItemStack.setCustomItemName(name: Component): ItemStack {
    this.set(DataComponents.CUSTOM_NAME, name)
    return this
}
//#endif

enum class DyeCompat(
    private val dyeColor: Int,
    //#if MC > 1.16
    private val stackType: Item
    //#endif
) {
    WHITE(
        15,
        //#if MC > 1.16
        Items.BONE_MEAL
        //#endif
    ),
    ORANGE(
        14,
        //#if MC > 1.16
        Items.ORANGE_DYE
        //#endif
    ),
    MAGENTA(
        13,
        //#if MC > 1.16
        Items.MAGENTA_DYE
        //#endif
    ),
    LIGHT_BLUE(
        12,
        //#if MC > 1.16
        Items.LIGHT_BLUE_DYE
        //#endif
    ),
    YELLOW(
        11,
        //#if MC > 1.16
        Items.YELLOW_DYE
        //#endif
    ),
    LIME(
        10,
        //#if MC > 1.16
        Items.LIME_DYE
        //#endif
    ),
    PINK(
        9,
        //#if MC > 1.16
        Items.PINK_DYE
        //#endif
    ),
    GRAY(
        8,
        //#if MC > 1.16
        Items.GRAY_DYE
        //#endif
    ),
    LIGHT_GRAY(
        7,
        //#if MC > 1.16
        Items.LIGHT_GRAY_DYE
        //#endif
    ),
    CYAN(
        6,
        //#if MC > 1.16
        Items.CYAN_DYE
        //#endif
    ),
    PURPLE(
        5,
        //#if MC > 1.16
        Items.PURPLE_DYE
        //#endif
    ),
    BLUE(
        4,
        //#if MC > 1.16
        Items.LAPIS_LAZULI
        //#endif
    ),
    BROWN(
        3,
        //#if MC > 1.16
        Items.COCOA_BEANS
        //#endif
    ),
    GREEN(
        2,
        //#if MC > 1.16
        Items.GREEN_DYE
        //#endif
    ),
    RED(
        1,
        //#if MC > 1.16
        Items.RED_DYE
        //#endif
    ),
    BLACK(
        0,
        //#if MC > 1.16
        Items.BLACK_DYE
        //#endif
    )
    ;

    fun createStack(size: Int = 1): ItemStack =
        //#if MC < 1.16
        //$$ ItemStack(Items.dye, size, dyeColor)
    //#else
    ItemStack(stackType, size)
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
                //$$ return this.item == Items.dye
                //#else
                return entries.firstOrNull { this.item == item } != null
                //#endif
            }

            //#if MC < 1.16
            //$$ return this.item == Items.dye && this.metadata == metadata
            //#else
            return this.item == fromDyeColor(metadata).stackType
            //#endif
        }

        private fun fromDyeColor(dyeColor: Int): DyeCompat = entries.firstOrNull { it.dyeColor == dyeColor } ?: GRAY

        fun toDamage(stack: ItemStack): Int {
            //#if MC < 1.21
            //$$ return entries.firstOrNull { it.dyeColor == stack.metadata }?.dyeColor ?: 0
            //#else
            return entries.firstOrNull { it.stackType == stack.item }?.dyeColor ?: 0
            //#endif
        }

        fun createDyeStack(dyeColor: Int, size: Int = 1): ItemStack =
            fromDyeColor(dyeColor).createStack(size)
    }
}
