package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.json.fromJsonOrNull
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

data class GridPosition(val x: Int, val y: Int)

// type can be 0, 1, 2. 0 = target, 1 = ingredient, 2 = unique crop bonus crop
data class SlotInfo(val crop: String, val type: Int, val surface: Block)

class SkyMutationsData(val input: String) {

    val grid by lazy { parseGrid() }

    fun String.getVanillaCropOrNull(): CropType? = if (this == "Wheat Seeds") CropType.WHEAT else CropType.getByNameOrNull(this)

    private fun String.getSurface(): Block {
        val vanillaCrop = getVanillaCropOrNull() ?: return when (this) {
            "Ashwreath", "Witherbloom", "Cindershade", "Zombud", "Phantomleaf" -> Blocks.SOUL_SAND
            "Veilshroom" -> Blocks.MYCELIUM
            "Blastberry", "Magic Jellybean", "All-in Aloe", "Glasscorn" -> Blocks.SAND
            "Chorus Fruit", "Timestalk" -> Blocks.END_STONE
            else -> Blocks.FARMLAND
        }

        return vanillaCrop.cropSurface
    }

    private fun parseGrid(): Map<GridPosition, SlotInfo> {

        val data = ConfigManager.gson.fromJsonOrNull<List<List<Any>>>(input) ?: return emptyMap()

        return data.associate { row ->

            val x = (row[0] as Number).toInt()
            val y = (row[1] as Number).toInt()
            val crop = row[2] as String
            val type = (row[3] as Number).toInt()

            GridPosition(x, y) to SlotInfo(crop, type, crop.getSurface())
        }
    }
}
