package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.json.fromJsonOrNull
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.item.FallingBlockEntity
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks


data class GridPosition(val x: Int, val y: Int)

data class SlotInfo(val crop: String, val type: GreenhouseCropRole, val surface: Block)

enum class GreenhouseCropRole {
    TARGET,
    INPUT;

    companion object {
        fun fromInt(value: Int) = entries.getOrElse(value) { INPUT }
    }
}

class GreenhouseData(val input: String) {

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

    private fun parseGrid(): Map<GridPosition, SlotInfo> = parseSkyMutations() ?: parseSkyShards()

    private fun parseSkyShards(): Map<GridPosition, SlotInfo> {
        val data = SkyShardsDecoder.decodeDesign(input) ?: return emptyMap()

        val grid = mutableMapOf<GridPosition, SlotInfo>()

        data.inputs.forEach {
            val crop = parseCropId(it.cropId)
            grid[it.position] = SlotInfo(crop, GreenhouseCropRole.INPUT, crop.getSurface())
        }
        data.targets.forEach {
            val crop = parseCropId(it.cropId)
            grid[it.position] = SlotInfo(crop, GreenhouseCropRole.TARGET, crop.getSurface())
        }

        return grid
    }

    private fun parseSkyMutations(): Map<GridPosition, SlotInfo>? {
        val decompressed = LZString.decompressFromEncodedURIComponent(input)
        val json = ConfigManager.gson.fromJsonOrNull<List<List<Any>>>(decompressed) ?: return null

        return json.associate { row ->

            val x = (row[0] as Number).toInt()
            val y = (row[1] as Number).toInt()
            val crop = row[2] as String
            val type = GreenhouseCropRole.fromInt(row[3] as Int)

            GridPosition(x, y) to SlotInfo(crop, type, crop.getSurface())
        }
    }

    private fun parseCropId(id: String): String {
        return when (id) {
            "do_not_eat_shroom" -> "Do-not-eat-shroom"
            "plantboy_advance" -> "PlantBoy Advance"
            "all_in_aloe" -> "All-in Aloe"
            else -> id.replace("_", "").firstLetterUppercase()
        }
    }
}
