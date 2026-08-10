package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.GardenStorage.GreenHouseStorage
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.annotations.Expose
import kotlin.math.floor

internal object GreenhouseBlueprintFile {

    data class ImportedLayout(
        val name: String,
        val blueprint: GreenHouseStorage.MutationBlueprintStorage,
    )

    private data class PortableLayout(
        @Expose val format: String = FILE_FORMAT,
        @Expose val version: Int = FILE_VERSION,
        @Expose val name: String = "",
        @Expose val blueprint: GreenHouseStorage.MutationBlueprintStorage? = null,
    )

    fun encode(name: String, blueprint: GreenHouseStorage.MutationBlueprintStorage): String {
        val portableBlueprint = GreenHouseStorage.MutationBlueprintStorage(
            minXOffset = blueprint.minXOffset,
            minZOffset = blueprint.minZOffset,
            maxXOffset = blueprint.maxXOffset,
            maxZOffset = blueprint.maxZOffset,
            mutations = blueprint.mutations.mapTo(mutableListOf()) { placement ->
                GreenHouseStorage.MutationPlacementStorage(
                    mutationId = placement.mutationId,
                    offset = placement.offset,
                    texture = "",
                    size = placement.size,
                )
            },
            importedCells = blueprint.importedCells.mapTo(mutableListOf()) { cell ->
                GreenHouseStorage.BlueprintCellStorage(
                    cropId = cell.cropId,
                    row = cell.row,
                    column = cell.column,
                    target = cell.target,
                )
            },
            targetMutationId = blueprint.targetMutationId,
        )
        return ConfigManager.gson.toJson(PortableLayout(name = name, blueprint = portableBlueprint))
    }

    fun decode(serialized: String): ImportedLayout {
        require(serialized.toByteArray().size <= MAX_FILE_BYTES) { "The layout file is too large." }
        val portable = runCatching { ConfigManager.gson.fromJson<PortableLayout>(serialized) }
            .getOrElse { throw IllegalArgumentException("The layout file does not contain valid JSON.", it) }
        require(portable.format == FILE_FORMAT) { "This is not a SkyHanni Greenhouse layout file." }
        require(portable.version == FILE_VERSION) {
            "Unsupported layout version ${portable.version}; expected version $FILE_VERSION."
        }
        val blueprint = requireNotNull(portable.blueprint) { "The layout file does not contain a layout." }
        require(blueprint.mutations.size <= MAX_GRID_ENTRIES) { "The layout contains too many mutations." }
        require(blueprint.importedCells.size <= MAX_GRID_ENTRIES) { "The layout contains too many grid entries." }
        require(blueprint.mutations.isNotEmpty() || blueprint.importedCells.isNotEmpty()) { "The layout is empty." }
        val occupiedMutationCells = mutableSetOf<Pair<Int, Int>>()
        blueprint.mutations.forEach { placement ->
            val mutation = GreenhouseMutation.fromInternalId(placement.mutationId)
            require(mutation != null) {
                "Unknown mutation ${placement.mutationId}."
            }
            require(placement.offset.x.isFinite() && placement.offset.y.isFinite() && placement.offset.z.isFinite()) {
                "Invalid mutation position."
            }
            require(placement.offset.x in -MAX_OFFSET..MAX_OFFSET && placement.offset.z in -MAX_OFFSET..MAX_OFFSET) {
                "A mutation is outside the Greenhouse grid."
            }
            require(placement.offset.y in MIN_Y_OFFSET..MAX_Y_OFFSET) { "A mutation has an invalid height." }
            val anchorColumn = floor(placement.offset.x).toInt() + GRID_RADIUS
            val anchorRow = floor(placement.offset.z).toInt() + GRID_RADIUS
            val topLeftColumn = anchorColumn - mutation.size / 2
            val topLeftRow = anchorRow - mutation.size / 2
            repeat(mutation.size) { rowOffset ->
                repeat(mutation.size) { columnOffset ->
                    val cell = topLeftRow + rowOffset to topLeftColumn + columnOffset
                    require(cell.first in 0 until GRID_SIZE && cell.second in 0 until GRID_SIZE) {
                        "A mutation footprint is outside the Greenhouse grid."
                    }
                    require(occupiedMutationCells.add(cell)) { "Mutation footprints overlap." }
                }
            }
            // Never trust a profile texture supplied by a shared file. The renderer falls back to
            // SkyHanni's mutation item when this is empty.
            placement.texture = ""
            placement.size = mutation.size
        }
        val occupiedCropCells = mutableSetOf<Pair<Int, Int>>()
        blueprint.importedCells.forEach { cell ->
            require(cell.cropId.length in 1..MAX_CROP_ID_LENGTH) { "Invalid crop ID." }
            require(cell.row in 0 until GRID_SIZE && cell.column in 0 until GRID_SIZE) {
                "A crop is outside the Greenhouse grid."
            }
            require(occupiedCropCells.add(cell.row to cell.column)) { "The layout contains duplicate grid entries." }
        }
        require(
            blueprint.targetMutationId.isEmpty() || blueprint.targetMutationId == NO_TARGET_MUTATION ||
                GreenhouseMutation.fromInternalId(blueprint.targetMutationId) != null,
        ) { "The layout has an unknown target mutation." }

        blueprint.minXOffset = -GRID_RADIUS
        blueprint.minZOffset = -GRID_RADIUS
        blueprint.maxXOffset = GRID_RADIUS - 1
        blueprint.maxZOffset = GRID_RADIUS - 1
        val safeName = portable.name.filterNot {
            it.isISOControl() || it == COLOR_CODE_MARKER || Character.getType(it) == Character.FORMAT.toInt()
        }.trim()
        return ImportedLayout(safeName, blueprint)
    }

    fun suggestedFileName(name: String): String {
        val safeName = name.replace(INVALID_FILE_NAME_CHARACTERS, "_").trim(' ', '.')
            .ifEmpty { DEFAULT_FILE_NAME }
        return "$safeName$FILE_EXTENSION"
    }

    fun withFileExtension(path: String): String = if (path.endsWith(FILE_EXTENSION, ignoreCase = true)) {
        path
    } else {
        "$path$FILE_EXTENSION"
    }

    const val FILE_EXTENSION = ".shgreenhouse.json"
    private const val FILE_FORMAT = "skyhanni-greenhouse-layout"
    private const val FILE_VERSION = 1
    private const val GRID_RADIUS = 5
    private const val GRID_SIZE = GRID_RADIUS * 2
    private const val MAX_GRID_ENTRIES = GRID_SIZE * GRID_SIZE
    internal const val MAX_FILE_BYTES = 1_048_576
    private const val MAX_OFFSET = 16.0
    private const val MIN_Y_OFFSET = 45.0
    private const val MAX_Y_OFFSET = 95.0
    private const val MAX_CROP_ID_LENGTH = 64
    private const val NO_TARGET_MUTATION = "NONE"
    private const val DEFAULT_FILE_NAME = "Greenhouse Layout"
    private const val COLOR_CODE_MARKER = '§'
    private val INVALID_FILE_NAME_CHARACTERS = "[<>:\"/\\|?*\\p{Cntrl}]".toRegex()
}
