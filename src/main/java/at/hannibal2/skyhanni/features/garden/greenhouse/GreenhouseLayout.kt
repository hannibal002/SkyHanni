package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseLayoutApi.GreenhouseCropRole
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseLayoutApi.GridPosition
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseLayoutApi.SlotInfo
import at.hannibal2.skyhanni.utils.json.fromJsonOrNull

class GreenhouseLayout(val input: String) {

    val grid by lazy { parseGrid() }

    private fun parseGrid(): MutableMap<GridPosition, SlotInfo> = parseSkyMutations() ?: parseSkyShards()

    private fun parseSkyShards(): MutableMap<GridPosition, SlotInfo> {
        val data = SkyShardsDecoder.decodeDesign(input) ?: return mutableMapOf()
        val grid = mutableMapOf<GridPosition, SlotInfo>()

        data.inputs.forEach {
            val crop = GreenhouseCropUtils.parseCropId(it.cropId)
            grid[it.position] = SlotInfo(crop, GreenhouseCropRole.INPUT, GreenhouseCropUtils.getSurface(crop))
        }
        data.targets.forEach {
            val crop = GreenhouseCropUtils.parseCropId(it.cropId)
            grid[it.position] = SlotInfo(crop, GreenhouseCropRole.TARGET, GreenhouseCropUtils.getSurface(crop))
        }

        return grid
    }

    private fun parseSkyMutations(): MutableMap<GridPosition, SlotInfo>? {
        val decompressed = LZString.decompressFromEncodedURIComponent(input)
        val json = ConfigManager.gson.fromJsonOrNull<List<List<Any>>>(decompressed) ?: return null

        return json.associate { row ->
            val x = (row[1] as Number).toInt()
            val y = (row[0] as Number).toInt()
            val crop = row[2] as String
            val type = GreenhouseCropRole.fromInt((row[3] as Number).toInt())

            GridPosition(x, y) to SlotInfo(crop, type, GreenhouseCropUtils.getSurface(crop))
        }.toMutableMap()
    }
}
