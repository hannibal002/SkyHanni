package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.features.garden.fortuneguide.FarmingItemType
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.compat.BlockCompat
import at.hannibal2.skyhanni.utils.compat.DyeCompat
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

enum class CropType(
    val cropName: String,
    val toolName: String,
    val specialDropType: String,
    val baseDrops: Double,
    iconSupplier: () -> ItemStack,
    val simpleName: String,
    val farmingItem: FarmingItemType,
    val replenish: Boolean = false,
    val enchantName: String = cropName.lowercase(),
) {

    WHEAT(
        "Wheat", "THEORETICAL_HOE_WHEAT", "CROPIE", 1.0,
        { ItemStack(Items.WHEAT) }, "wheat", FarmingItemType.WHEAT,
    ),
    CARROT(
        "Carrot", "THEORETICAL_HOE_CARROT", "CROPIE", 3.0,
        { ItemStack(Items.CARROT) }, "carrot", FarmingItemType.CARROT, replenish = true,
    ),
    POTATO(
        "Potato", "THEORETICAL_HOE_POTATO", "CROPIE", 3.0,
        { ItemStack(Items.POTATO) }, "potato", FarmingItemType.POTATO, replenish = true,
    ),
    NETHER_WART(
        "Nether Wart", "THEORETICAL_HOE_WARTS", "FERMENTO", 2.5,
        { ItemStack(Items.NETHER_WART) }, "wart", FarmingItemType.NETHER_WART, replenish = true,
        enchantName = "warts",
    ),
    PUMPKIN(
        "Pumpkin", "PUMPKIN_DICER", "SQUASH", 1.0,
        { ItemStack(Blocks.CARVED_PUMPKIN) }, "pumpkin", FarmingItemType.PUMPKIN,
    ),
    MELON(
        "Melon Slice", "MELON_DICER", "SQUASH", 5.0,
        { ItemStack(Items.MELON_SLICE) }, "melon", FarmingItemType.MELON,
    ),
    COCOA_BEANS(
        "Cocoa Beans", "COCO_CHOPPER", "SQUASH", 3.0,
        { DyeCompat.BROWN.createStack() }, "cocoa",
        FarmingItemType.COCOA_BEANS, replenish = true, enchantName = "coco",
    ),
    SUGAR_CANE(
        "Sugar Cane", "THEORETICAL_HOE_CANE", "FERMENTO", 2.0,
        { ItemStack(Items.SUGAR_CANE) }, "cane", FarmingItemType.SUGAR_CANE, enchantName = "cane",
    ),
    CACTUS(
        "Cactus", "CACTUS_KNIFE", "FERMENTO", 2.0,
        { ItemStack(Blocks.CACTUS) }, "cactus", FarmingItemType.CACTUS,
    ),
    MUSHROOM(
        "Mushroom", "FUNGI_CUTTER", "FERMENTO", 1.0,
        { ItemStack(Blocks.RED_MUSHROOM_BLOCK) }, "mushroom", FarmingItemType.MUSHROOM,
        enchantName = "mushrooms",
    ),
    SUNFLOWER(
        "Sunflower", "THEORETICAL_HOE_SUNFLOWER", "HELIANTHUS", 2.0,
        { BlockCompat.createSunFlower() }, "sunflower", FarmingItemType.SUNFLOWER,
    ),
    MOONFLOWER(
        "Moonflower", "THEORETICAL_HOE_SUNFLOWER", "HELIANTHUS", 2.0,
        { BlockCompat.createBlueOrchid() }, "moonflower", FarmingItemType.MOONFLOWER,
    ),
    WILD_ROSE(
        "Wild Rose", "THEORETICAL_HOE_WILD_ROSE", "HELIANTHUS", 2.0,
        { BlockCompat.createWildRose() }, "rose", FarmingItemType.WILD_ROSE,
    ),
    ;

    val icon by lazy { iconSupplier() }

    val multiplier by lazy { if (this == SUGAR_CANE || this == CACTUS) 2 else 1 }

    override fun toString(): String = cropName

    val patternKeyName = name.lowercase().replace('_', '.')
    val niceName = name.lowercase().replace('_', ' ')

    companion object {

        fun getByNameOrNull(itemName: String): CropType? {
            if (itemName == "Red Mushroom" || itemName == "Brown Mushroom") return MUSHROOM
            if (itemName == "Seeds") return WHEAT
            return entries.firstOrNull {
                it.cropName.equals(itemName, ignoreCase = true) ||
                    it.simpleName.equals(itemName, ignoreCase = true) ||
                    it.enchantName.equals(itemName, ignoreCase = true)
            }
        }

        fun getByName(name: String) = getByNameOrNull(name) ?: error("No valid crop type '$name'")

        fun BlockState.getCropType(pos: LorenzVec): CropType? {
            return when (block) {
                Blocks.WHEAT -> WHEAT
                Blocks.CARROTS -> CARROT
                Blocks.POTATOES -> POTATO
                Blocks.CARVED_PUMPKIN -> PUMPKIN
                Blocks.SUGAR_CANE -> SUGAR_CANE
                Blocks.MELON -> MELON
                Blocks.CACTUS -> CACTUS
                Blocks.COCOA -> COCOA_BEANS
                Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM -> MUSHROOM
                Blocks.NETHER_WART -> NETHER_WART
                Blocks.ROSE_BUSH -> WILD_ROSE
                Blocks.SUNFLOWER -> getTimeFlower()
                else -> null
            }
        }

        fun getTimeFlower(): CropType {
            val time = MinecraftCompat.localWorld.dayTime % 24000
            // pretty sure great spook will break this
            return if (time >= 12000) MOONFLOWER else SUNFLOWER
        }

        fun CropType?.isTimeFlower(): Boolean {
            return this == SUNFLOWER || this == MOONFLOWER
        }

        fun CropType.getTurboCrop() = "turbo_${this.enchantName.lowercase()}"
    }
}
