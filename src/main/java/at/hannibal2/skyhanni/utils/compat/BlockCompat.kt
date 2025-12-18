package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.block.Block
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
//#if MC < 1.21
import net.minecraft.block.properties.IProperty
//#endif

object BlockCompat {
    fun getAllLogs(): List<Block> {
        val logs = mutableListOf<Block>()
        logs.addLog()
        logs.addLog2()
        //#if MC > 1.21
        //$$ logs.addModernLogs()
        //#endif
        return logs
    }

    fun createSmoothDiorite(): ItemStack {
        //#if MC < 1.21
        return ItemStack(Blocks.stone, 1, net.minecraft.block.BlockStone.EnumType.DIORITE_SMOOTH.metadata)
        //#else
        //$$ return ItemStack(Blocks.POLISHED_DIORITE)
        //#endif
    }

    fun createBlueOrchid(): ItemStack {
        //#if MC < 1.21
        return ItemStack(Blocks.red_flower, 1, 1)
        //#else
        //$$ return ItemStack(Blocks.BLUE_ORCHID)
        //#endif
    }

    fun createSunFlower(): ItemStack {
        //#if MC < 1.21
        return ItemStack(Blocks.double_plant)
        //#else
        //$$ return ItemStack(Blocks.SUNFLOWER)
        //#endif
    }

    fun createWildRose(): ItemStack {
        //#if MC < 1.21
        return ItemStack(Blocks.double_plant, 1, 4)
        //#else
        //$$ return ItemStack(Blocks.ROSE_BUSH)
        //#endif
    }

    @Suppress("ReturnCount")
    fun IBlockState.getFlowerType(pos: LorenzVec): String? {
        //#if MC < 1.21
        val property = (this.block.blockState.properties.find { it.name == "variant" } as? PropertyEnum) ?: return null
        val halfProperty = (this.block.blockState.properties.find { it.name == "half" } as? PropertyEnum) ?: return null
        val flower = getValue(property as? IProperty<*>).toString()
        val upper = getValue(halfProperty as? IProperty<*>)
        if (upper.toString() == "upper") {
            val lowerState = pos.down(1).getBlockStateAt()
            // this is really cursed
            // fuck blockstates
            val stateString = lowerState.toString()
            if (stateString.contains("variant=sunflower")) return "sunflower"
            if (stateString.contains("variant=syringa")) return "syringa"
            if (stateString.contains("variant=double_grass")) return "double_grass"
            if (stateString.contains("variant=double_fern")) return "double_fern"
            if (stateString.contains("variant=double_rose")) return "double_rose"
            if (stateString.contains("variant=paeonia")) return "paeonia"
        }
        return flower
        //#else
        //$$ return "dont use on 1.21"
        //#endif
    }

    fun IBlockState.isSunflower(pos: LorenzVec): Boolean {
        //#if MC < 1.21
        return this.getFlowerType(pos) == "sunflower"
        //#else
        //$$ return this.block == Blocks.SUNFLOWER
        //#endif
    }

    fun IBlockState.isWildRose(pos: LorenzVec): Boolean {
        //#if MC < 1.21
        return this.getFlowerType(pos) == "double_rose"
        //#else
        //$$ return this.block == Blocks.ROSE_BUSH
        //#endif
    }

    fun getAllLeaves(): List<Block> = buildList { addLeaves() }
}

fun MutableList<Block>.addLeaves() {
    //#if MC < 1.21
    this.add(Blocks.leaves)
    //#else
    //$$ this.add(Blocks.OAK_LEAVES)
    //$$ this.add(Blocks.SPRUCE_LEAVES)
    //$$ this.add(Blocks.BIRCH_LEAVES)
    //$$ this.add(Blocks.JUNGLE_LEAVES)
    //#endif
}

fun MutableList<Block>.addLeaves2() {
    //#if MC < 1.21
    this.add(Blocks.leaves2)
    //#else
    //$$ this.add(Blocks.ACACIA_LEAVES)
    //$$ this.add(Blocks.DARK_OAK_LEAVES)
    //#endif
}

fun MutableList<Block>.addTallGrass() {
    //#if MC < 1.21
    this.add(Blocks.tallgrass)
    //#else
    //$$ this.add(Blocks.SHORT_GRASS)
    //$$ this.add(Blocks.FERN)
    //#endif
}

fun MutableList<Block>.addDoublePlant() {
    //#if MC < 1.21
    this.add(Blocks.double_plant)
    //#else
    //$$ this.add(Blocks.SUNFLOWER)
    //$$ this.add(Blocks.LILAC)
    //$$ this.add(Blocks.TALL_GRASS)
    //$$ this.add(Blocks.LARGE_FERN)
    //$$ this.add(Blocks.ROSE_BUSH)
    //$$ this.add(Blocks.PEONY)
    //#endif
}

fun MutableList<Block>.addRedFlower() {
    //#if MC < 1.21
    this.add(Blocks.red_flower)
    //#else
    //$$ this.add(Blocks.POPPY)
    //$$ this.add(Blocks.BLUE_ORCHID)
    //$$ this.add(Blocks.ALLIUM)
    //$$ this.add(Blocks.AZURE_BLUET)
    //$$ this.add(Blocks.RED_TULIP)
    //$$ this.add(Blocks.ORANGE_TULIP)
    //$$ this.add(Blocks.WHITE_TULIP)
    //$$ this.add(Blocks.PINK_TULIP)
    //$$ this.add(Blocks.OXEYE_DAISY)
    //#endif
}

fun MutableList<Block>.addRedstoneOres() {
    this.add(Blocks.redstone_ore)
    //#if MC < 1.16
    this.add(Blocks.lit_redstone_ore)
    //#endif
}

fun MutableList<Block>.addWaters() {
    this.add(Blocks.water)
    //#if MC < 1.16
    this.add(Blocks.flowing_water)
    //#endif
}

fun MutableList<Block>.addLavas() {
    this.add(Blocks.lava)
    //#if MC < 1.16
    this.add(Blocks.flowing_lava)
    //#endif
}

fun MutableList<Block>.addLog() {
    //#if MC < 1.16
    this.add(Blocks.log)
    //#else
    //$$ this.add(Blocks.OAK_LOG)
    //$$ this.add(Blocks.OAK_WOOD)
    //$$ this.add(Blocks.SPRUCE_LOG)
    //$$ this.add(Blocks.SPRUCE_WOOD)
    //$$ this.add(Blocks.BIRCH_LOG)
    //$$ this.add(Blocks.BIRCH_WOOD)
    //$$ this.add(Blocks.JUNGLE_LOG)
    //$$ this.add(Blocks.JUNGLE_WOOD)
    //#endif
}

fun MutableList<Block>.addLog2() {
    //#if MC < 1.16
    this.add(Blocks.log2)
    //#else
    //$$ this.add(Blocks.ACACIA_LOG)
    //$$ this.add(Blocks.ACACIA_WOOD)
    //$$ this.add(Blocks.DARK_OAK_LOG)
    //$$ this.add(Blocks.DARK_OAK_WOOD)
    //#endif
}

//#if MC > 1.21
//$$ fun MutableList<Block>.addModernLogs() {
//$$     this.add(Blocks.WARPED_STEM)
//$$     this.add(Blocks.WARPED_HYPHAE)
//$$     this.add(Blocks.CRIMSON_STEM)
//$$     this.add(Blocks.CRIMSON_HYPHAE)
//$$     this.add(Blocks.MANGROVE_LOG)
//$$     this.add(Blocks.MANGROVE_WOOD)
//$$     this.add(Blocks.CHERRY_LOG)
//$$     this.add(Blocks.CHERRY_WOOD)
//$$     this.add(Blocks.PALE_OAK_LOG)
//$$     this.add(Blocks.PALE_OAK_WOOD)
//$$ }
//#endif
