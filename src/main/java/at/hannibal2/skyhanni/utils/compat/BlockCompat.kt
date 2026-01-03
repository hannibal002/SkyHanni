package at.hannibal2.skyhanni.utils.compat

import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

object BlockCompat {
    fun getAllLogs(): List<Block> {
        val logs = mutableListOf<Block>()
        logs.addLog()
        logs.addLog2()
        logs.addModernLogs()
        return logs
    }

    fun createSmoothDiorite(): ItemStack {
        return ItemStack(Blocks.POLISHED_DIORITE)
    }

    fun getAllLeaves(): List<Block> = buildList { addLeaves() }
}

fun MutableList<Block>.addLeaves() {
    this.add(Blocks.OAK_LEAVES)
    this.add(Blocks.SPRUCE_LEAVES)
    this.add(Blocks.BIRCH_LEAVES)
    this.add(Blocks.JUNGLE_LEAVES)
}

fun MutableList<Block>.addLeaves2() {
    this.add(Blocks.ACACIA_LEAVES)
    this.add(Blocks.DARK_OAK_LEAVES)
}

fun MutableList<Block>.addTallGrass() {
    this.add(Blocks.SHORT_GRASS)
    this.add(Blocks.FERN)
}

fun MutableList<Block>.addDoublePlant() {
    this.add(Blocks.SUNFLOWER)
    this.add(Blocks.LILAC)
    this.add(Blocks.TALL_GRASS)
    this.add(Blocks.LARGE_FERN)
    this.add(Blocks.ROSE_BUSH)
    this.add(Blocks.PEONY)
}

fun MutableList<Block>.addRedFlower() {
    this.add(Blocks.POPPY)
    this.add(Blocks.BLUE_ORCHID)
    this.add(Blocks.ALLIUM)
    this.add(Blocks.AZURE_BLUET)
    this.add(Blocks.RED_TULIP)
    this.add(Blocks.ORANGE_TULIP)
    this.add(Blocks.WHITE_TULIP)
    this.add(Blocks.PINK_TULIP)
    this.add(Blocks.OXEYE_DAISY)
}

fun MutableList<Block>.addRedstoneOres() {
    this.add(Blocks.REDSTONE_ORE)
}

fun MutableList<Block>.addWaters() {
    this.add(Blocks.WATER)
}

fun MutableList<Block>.addLavas() {
    this.add(Blocks.LAVA)
}

fun MutableList<Block>.addLog() {
    this.add(Blocks.OAK_LOG)
    this.add(Blocks.OAK_WOOD)
    this.add(Blocks.SPRUCE_LOG)
    this.add(Blocks.SPRUCE_WOOD)
    this.add(Blocks.BIRCH_LOG)
    this.add(Blocks.BIRCH_WOOD)
    this.add(Blocks.JUNGLE_LOG)
    this.add(Blocks.JUNGLE_WOOD)
}

fun MutableList<Block>.addLog2() {
    this.add(Blocks.ACACIA_LOG)
    this.add(Blocks.ACACIA_WOOD)
    this.add(Blocks.DARK_OAK_LOG)
    this.add(Blocks.DARK_OAK_WOOD)
}

fun MutableList<Block>.addModernLogs() {
    this.add(Blocks.WARPED_STEM)
    this.add(Blocks.WARPED_HYPHAE)
    this.add(Blocks.CRIMSON_STEM)
    this.add(Blocks.CRIMSON_HYPHAE)
    this.add(Blocks.MANGROVE_LOG)
    this.add(Blocks.MANGROVE_WOOD)
    this.add(Blocks.CHERRY_LOG)
    this.add(Blocks.CHERRY_WOOD)
    this.add(Blocks.PALE_OAK_LOG)
    this.add(Blocks.PALE_OAK_WOOD)
}
