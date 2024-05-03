package at.hannibal2.skyhanni.features.mining

import net.minecraft.block.BlockSand
import net.minecraft.block.BlockStone
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack

enum class OreType(
    val oreName: String,
    val item: ItemStack,
    val shaftMultiplier: Int = 0,
) {
    MITHRIL("Mithril", ItemStack(Blocks.wool, 1, EnumDyeColor.LIGHT_BLUE.metadata), 2),
    TITANIUM("Titanium", ItemStack(Blocks.stone, 1, BlockStone.EnumType.DIORITE_SMOOTH.metadata), 8),
    COBBLESTONE("Cobblestone", ItemStack(Blocks.cobblestone)),
    COAL("Coal", ItemStack(Blocks.coal_block)),
    IRON("Iron", ItemStack(Blocks.iron_block)),
    GOLD("Gold", ItemStack(Blocks.gold_block)),
    LAPIS("Lapis Lazuli", ItemStack(Blocks.lapis_block)),
    REDSTONE("Redstone", ItemStack(Blocks.redstone_block)),
    EMERALD("Emerald", ItemStack(Blocks.emerald_block)),
    DIAMOND("Diamond", ItemStack(Blocks.diamond_block)),
    NETHERRACK("Netherrack", ItemStack(Blocks.netherrack)),
    QUARTZ("Nether Quartz", ItemStack(Blocks.quartz_block)),
    GLOWSTONE("Glowstone", ItemStack(Blocks.glowstone)),
    MYCELIUM("Mycelium", ItemStack(Blocks.mycelium)),
    RED_SAND("Red Sand", ItemStack(Blocks.sand, 1, BlockSand.EnumType.RED_SAND.metadata)),
    SULPHUR("Sulphur", ItemStack(Blocks.sponge)),
    GRAVEL("Gravel", ItemStack(Blocks.gravel)),
    END_STONE("End Stone", ItemStack(Blocks.end_stone)),
    OBSIDIAN("Obsidian", ItemStack(Blocks.obsidian)),
    HARD_STONE("Hard Stone", ItemStack(Blocks.stone)),
    GEMSTONE("Gemstone", ItemStack(Blocks.stained_glass, 1, EnumDyeColor.RED.metadata), 4),
    UMBER("Umber", ItemStack(Blocks.red_sandstone), 4),
    TUNGSTEN("Tungsten", ItemStack(Blocks.clay), 4),
    GLACITE("Glacite", ItemStack(Blocks.packed_ice), 4),
    ;
}
