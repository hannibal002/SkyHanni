package at.hannibal2.skyhanni.features.garden

import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack

enum class CropType(val cropName: String, val toolName: String, iconSupplier: () -> ItemStack) {
    WHEAT("Wheat", "THEORETICAL_HOE_WHEAT", { ItemStack(Items.wheat) }),
    CARROT("Carrot", "THEORETICAL_HOE_CARROT", { ItemStack(Items.carrot) }),
    POTATO("Potato", "THEORETICAL_HOE_POTATO", { ItemStack(Items.potato) }),
    NETHER_WART("Nether Wart", "THEORETICAL_HOE_WARTS", { ItemStack(Items.nether_wart) }),
    PUMPKIN("Pumpkin", "PUMPKIN_DICER", { ItemStack(Blocks.pumpkin) }),
    MELON("Melon", "MELON_DICER", { ItemStack(Items.melon) }),
    COCOA_BEANS("Cocoa Beans", "COCO_CHOPPER", { ItemStack(Items.dye, 1, EnumDyeColor.BROWN.dyeDamage) }),
    SUGAR_CANE("Sugar Cane", "THEORETICAL_HOE_CANE", { ItemStack(Items.reeds) }),
    CACTUS("Cactus", "CACTUS_KNIFE", { ItemStack(Blocks.cactus) }),
    MUSHROOM("Mushroom", "FUNGI_CUTTER", { ItemStack(Blocks.red_mushroom_block) }),
    ;

    val icon by lazy { iconSupplier() }

    companion object {
        fun getByName(cropName: String) = values().firstOrNull { it.cropName == cropName }

        // TODO find better name for this method
        fun getByNameNoNull(name: String) = getByName(name) ?: throw RuntimeException("No valid crop type '$name'")


        fun getByItemName(itemName: String): CropType? {
            if (itemName == "Red Mushroom" || itemName == "Brown Mushroom") {
                return MUSHROOM
            }
            return getByName(itemName)
        }

        fun getByBlock(blockState: IBlockState): CropType? {
            return when (blockState.block) {
                Blocks.wheat -> WHEAT
                Blocks.carrots -> CARROT
                Blocks.potatoes -> POTATO
                Blocks.pumpkin -> PUMPKIN
                Blocks.reeds -> SUGAR_CANE
                Blocks.melon_block -> MELON
                Blocks.cactus -> CACTUS
                Blocks.cocoa -> COCOA_BEANS
                Blocks.red_mushroom -> MUSHROOM
                Blocks.brown_mushroom -> MUSHROOM
                Blocks.nether_wart -> NETHER_WART
                else -> null
            }
        }

        fun CropType.getTurboCrop(): String {
            return when (this) {
                COCOA_BEANS -> "turbo_coco"
                SUGAR_CANE -> "turbo_cane"
                NETHER_WART -> "turbo_warts"
                MUSHROOM -> "turbo_mushrooms"
                else -> "turbo_${this.cropName.lowercase()}"
            }
        }
    }
}