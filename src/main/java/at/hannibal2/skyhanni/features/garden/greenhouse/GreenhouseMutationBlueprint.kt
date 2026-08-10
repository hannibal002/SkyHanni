package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.GardenStorage.GreenHouseStorage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.features.garden.plot.GardenPlot
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.isInLoadedChunk
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DialogUtils
import at.hannibal2.skyhanni.utils.ItemUtils.createSkull
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.position
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.LightCoordsUtil.FULL_BRIGHT
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.phys.AABB
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.math.floor

@SkyHanniModule
object GreenhouseMutationBlueprint {

    private val config get() = SkyHanniMod.feature.garden.greenhouse
    private val greenhouseStorage get() = ProfileStorageData.profileSpecific?.garden?.greenhouse

    private var previousMissing: MissingState? = null
    private var stableMissingChecks = 0
    private var missingPlacements: Set<Int> = emptySet()
    private var missingCropCells: Set<Int> = emptySet()
    private var activePlotId: Int? = null
    private val ghostStacks = mutableMapOf<String, SafeItemStack>()

    @HandleEvent(SecondPassedEvent::class, onlyOnIsland = IslandType.GARDEN)
    private fun onSecondPassed() {
        if (!config.mutationBlueprint || !GreenhouseUtils.isInGreenhouse()) {
            resetRuntimeState()
            return
        }
        val plot = currentGreenhousePlot() ?: return
        if (activePlotId != plot.id) {
            activePlotId = plot.id
            previousMissing = null
            stableMissingChecks = 0
            missingPlacements = emptySet()
            missingCropCells = emptySet()
        }
        val blueprint = activeBlueprint(plot) ?: run {
            missingPlacements = emptySet()
            missingCropCells = emptySet()
            return
        }
        val area = greenhouseArea(plot)
        if (!area.isLoaded()) return

        val liveMutations = GreenhouseMutationScanner.scan(area).toMutableList()
        val targetMutation = blueprint.targetMutation()
        val missingMutations = buildSet {
            blueprint.mutations.forEachIndexed { index, saved ->
                val mutation = GreenhouseMutation.fromInternalId(saved.mutationId) ?: return@forEachIndexed
                if (mutation == targetMutation) return@forEachIndexed
                val expected = saved.worldPosition(plot, blueprint.importedCells.isNotEmpty())
                val matchIndex = liveMutations.indices.minByOrNull { liveIndex ->
                    val live = liveMutations[liveIndex]
                    if (live.mutation != mutation) Double.MAX_VALUE else live.position.distanceSq(expected)
                }
                val match = matchIndex?.let(liveMutations::get)
                if (match == null || match.mutation != mutation || !match.position.matchesAnchor(expected, saved.size)) {
                    add(index)
                } else {
                    liveMutations.removeAt(matchIndex)
                }
            }
        }
        val missingCrops = buildSet {
            blueprint.importedCells.forEachIndexed { index, cell ->
                if (cell.target || GreenhouseMutation.fromSkyShardsId(cell.cropId) != null) return@forEachIndexed
                val category = cell.cropCategory() ?: return@forEachIndexed
                if (GreenhouseCropScanner.isMissingCrop(cell.worldPosition(plot, cropBlock = true), category)) add(index)
            }
        }
        val missing = MissingState(missingMutations, missingCrops)

        if (missing != previousMissing) {
            previousMissing = missing
            stableMissingChecks = 1
            return
        }
        stableMissingChecks++
        if (stableMissingChecks >= REQUIRED_STABLE_CHECKS) {
            missingPlacements = missing.mutations
            missingCropCells = missing.crops
        }
    }

    @HandleEvent(WorldChangeEvent::class)
    private fun onWorldChange() = resetRuntimeState()

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.mutationBlueprint || !GreenhouseUtils.isInGreenhouse()) return
        val plot = currentGreenhousePlot() ?: return

        val blueprint = activeBlueprint(plot) ?: return
        missingPlacements.forEach { index ->
            val saved = blueprint.mutations.getOrNull(index) ?: return@forEach
            val mutation = GreenhouseMutation.fromInternalId(saved.mutationId) ?: return@forEach
            val position = saved.worldPosition(plot, blueprint.importedCells.isNotEmpty())
            event.drawWaypointFilled(
                position,
                LorenzColor.RED.addOpacity(90),
                seeThroughBlocks = true,
                minimumAlpha = 0.25f,
            )
            renderMutationHead(event, mutation, position, saved.texture)
            event.drawDynamicText(
                position.add(y = 1.4),
                "§cMissing ${mutation.displayName}",
                1.25,
            )
        }
        missingCropCells.forEach { index ->
            val cell = blueprint.importedCells.getOrNull(index) ?: return@forEach
            val category = cell.cropCategory() ?: return@forEach
            val position = cell.worldPosition(plot, cropBlock = true)
            event.drawWaypointFilled(
                position,
                LorenzColor.RED.addOpacity(80),
                seeThroughBlocks = true,
                minimumAlpha = 0.25f,
            )
            event.drawDynamicText(
                position.add(x = 0.5, y = 1.2, z = 0.5),
                "§cMissing ${category.displayName}",
                1.1,
            )
        }
    }

    private fun renderMutationHead(
        event: SkyHanniRenderWorldEvent,
        mutation: GreenhouseMutation,
        position: LorenzVec,
        texture: String,
    ) {
        val stackKey = "${mutation.internalId}:$texture"
        val stack = ghostStacks[stackKey] ?: run {
            val created = if (texture.isNotEmpty()) {
                val uuid = UUID.nameUUIDFromBytes((mutation.internalId + texture).toByteArray(StandardCharsets.UTF_8))
                createSkull(mutation.displayName, uuid.toString(), texture)
            } else {
                mutation.internalId.toInternalName().getItemStackOrNull() ?: return
            }
            ghostStacks[stackKey] = created
            created
        }
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return
        val renderState = ItemStackRenderState()
        minecraft.itemModelResolver.updateForTopItem(
            renderState,
            stack,
            ItemDisplayContext.FIXED,
            level,
            null,
            0,
        )
        event.matrices.pushPose()
        event.matrices.translate(
            position.x - event.camera.position.x,
            position.y - event.camera.position.y + 0.5,
            position.z - event.camera.position.z,
        )
        event.matrices.scale(1.2f, 1.2f, 1.2f)
        renderState.submit(
            event.matrices,
            minecraft.gameRenderer.featureRenderDispatcher.submitNodeStorage,
            FULL_BRIGHT,
            OverlayTexture.pack(0.8f, false),
            0,
        )
        event.matrices.popPose()
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shgreenhouseblueprint") {
            description = "Record and monitor mutations in the current Greenhouse plot"
            category = CommandCategory.USERS_ACTIVE
            simpleCallback { openLayoutLibrary() }
            literal("capture") {
                simpleCallback { captureGreenhouse() }
                argCallback("name", BrigadierArguments.greedyString()) { name -> captureGreenhouse(name) }
            }
            literalCallback("import") { importFromClipboard() }
            literal("target") {
                argCallback("mutation", BrigadierArguments.greedyString()) { mutation -> setTarget(mutation) }
            }
            literalCallback("clear") { clearCurrentBlueprint() }
            literalCallback("status") { showStatus() }
        }
    }

    private fun openLayoutLibrary() {
        val plot = currentGreenhousePlot() ?: run {
            ChatUtils.chat("§cYou must be standing in a Greenhouse plot.")
            return
        }
        migrateLegacyBlueprints()
        SkyHanniMod.screenToOpen = GreenhouseBlueprintScreen(plot.id)
    }

    internal fun captureGreenhouse(requestedName: String? = null) {
        val plot = currentGreenhousePlot() ?: run {
            ChatUtils.chat("§cYou must be standing in a Greenhouse plot.")
            return
        }
        if (!config.mutationBlueprint) {
            ChatUtils.chat("§cEnable §eMutation Blueprint §cin the Greenhouse settings first.")
            return
        }
        val area = greenhouseArea(plot)
        if (!area.isLoaded()) {
            ChatUtils.chat("§cThe Greenhouse planting grid must be loaded before it can be captured.")
            return
        }

        val detected = GreenhouseMutationScanner.scan(area)
            .distinctBy { Triple(it.mutation.internalId, floor(it.position.x).toInt(), floor(it.position.z).toInt()) }
        val capturedCells = GreenhouseLayoutCapture.captureCells(plot, detected)
        if (capturedCells.isEmpty()) {
            ChatUtils.chat("§cNo recognized crops or mutations were found in the 10x10 planting grid.")
            return
        }

        val inferredTarget = GreenhouseLayoutAnalysis.inferTarget(
            capturedCells.map { cell ->
                val size = GreenhouseMutation.fromSkyShardsId(cell.cropId)?.size ?: 1
                GreenhouseLayoutAnalysis.Entry(cell.cropId, size * size)
            },
        )
        val plotMiddle = plot.middle.toBlockPos()
        val blueprint = GreenHouseStorage.MutationBlueprintStorage(
            minXOffset = floor(area.minX).toInt() - plotMiddle.x,
            minZOffset = floor(area.minZ).toInt() - plotMiddle.z,
            maxXOffset = floor(area.maxX).toInt() - 1 - plotMiddle.x,
            maxZOffset = floor(area.maxZ).toInt() - 1 - plotMiddle.z,
            mutations = detected.mapTo(mutableListOf()) { detectedMutation ->
                GreenHouseStorage.MutationPlacementStorage(
                    mutationId = detectedMutation.mutation.internalId,
                    offset = detectedMutation.position - plot.middle,
                    texture = detectedMutation.texture,
                    size = detectedMutation.mutation.size,
                )
            },
            importedCells = capturedCells.toMutableList(),
            targetMutationId = inferredTarget?.internalId.orEmpty(),
        )
        val layouts = layoutMap() ?: run {
            ChatUtils.chat("§cYour SkyBlock profile storage is not available yet.")
            return
        }
        val name = requestedName?.trim()?.take(MAX_LAYOUT_NAME_LENGTH)?.takeIf(String::isNotEmpty)
            ?: nextLayoutName(layouts.keys)
        layouts[name] = blueprint
        activeLayoutMap()?.set(plot.id, name)
        SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "greenhouse-mutation-blueprint")
        resetMissingState()

        val summary = detected.groupingBy { it.mutation }.eachCount().entries
            .sortedBy { it.key.displayName }
            .joinToString("§7, §e") { (mutation, count) ->
                if (count == 1) mutation.displayName else "${mutation.displayName} x$count"
            }
        ChatUtils.chat(
            "§aSaved layout §e$name §awith §e${detected.size} §aGreenhouse mutations and loaded it in Plot " +
                "§e${plot.id}§a: §e$summary§a." + targetMessage(inferredTarget),
        )
    }

    @Suppress("ReturnCount")
    internal fun importFromClipboard() {
        val plot = currentGreenhousePlot() ?: run {
            ChatUtils.chat("§cYou must be standing in a Greenhouse plot.")
            return
        }
        if (!config.mutationBlueprint) {
            ChatUtils.chat("§cEnable §eMutation Blueprint §cin the Greenhouse settings first.")
            return
        }
        val clipboard = OSUtils.readFromClipboard() ?: run {
            ChatUtils.chat("§cThe clipboard does not contain text.")
            return
        }
        val imported = try {
            SkyShardsLayoutCodec.decode(clipboard)
        } catch (exception: IllegalArgumentException) {
            ChatUtils.chat("§cCould not import the SkyShards layout: §7${exception.message}")
            return
        }
        if (imported.placements.isEmpty()) {
            ChatUtils.chat("§cThe SkyShards layout is empty.")
            return
        }

        val mutations = imported.inputs.mapNotNull { placement ->
            val mutation = GreenhouseMutation.fromSkyShardsId(placement.cropId) ?: return@mapNotNull null
            val anchorOffset = mutation.size / 2
            GreenHouseStorage.MutationPlacementStorage(
                mutationId = mutation.internalId,
                offset = LorenzVec(
                    placement.column - GREENHOUSE_GRID_RADIUS + anchorOffset.toDouble(),
                    IMPORTED_MUTATION_Y - plot.middle.y,
                    placement.row - GREENHOUSE_GRID_RADIUS + anchorOffset.toDouble(),
                ),
                size = mutation.size,
            )
        }.toMutableList()
        val cells = imported.placements.mapTo(mutableListOf()) { placement ->
            GreenHouseStorage.BlueprintCellStorage(
                cropId = placement.cropId,
                row = placement.row,
                column = placement.column,
                target = placement.target,
            )
        }
        val blueprint = GreenHouseStorage.MutationBlueprintStorage(
            minXOffset = -GREENHOUSE_GRID_RADIUS,
            minZOffset = -GREENHOUSE_GRID_RADIUS,
            maxXOffset = GREENHOUSE_GRID_RADIUS - 1,
            maxZOffset = GREENHOUSE_GRID_RADIUS - 1,
            mutations = mutations,
            importedCells = cells,
            targetMutationId = GreenhouseLayoutAnalysis.inferTarget(
                imported.placements.map { placement ->
                    GreenhouseLayoutAnalysis.Entry(
                        placement.cropId,
                        GreenhouseMutation.fromSkyShardsId(placement.cropId)?.size?.let { it * it } ?: 1,
                        placement.target,
                    )
                },
            )?.internalId.orEmpty(),
        )
        val layouts = layoutMap() ?: run {
            ChatUtils.chat("§cYour SkyBlock profile storage is not available yet.")
            return
        }
        val name = nextLayoutName(layouts.keys, "SkyShards")
        layouts[name] = blueprint
        activeLayoutMap()?.set(plot.id, name)
        resetMissingState()
        SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "import-skyshards-greenhouse-layout")

        val cropCount = cells.count { !it.target && it.cropCategory() != null }
        val unsupportedCount = imported.inputs.size - mutations.size - cropCount
        ChatUtils.chat(
            "§aImported and loaded §e$name§a: §e${mutations.size} mutations§a, §e$cropCount crops§a, and " +
                "§e${imported.targets.size} target cells§a." +
                if (unsupportedCount > 0) " §7Ignored $unsupportedCount unsupported status cells." else "",
        )
    }

    private fun clearCurrentBlueprint() {
        val plot = currentGreenhousePlot() ?: run {
            ChatUtils.chat("§cYou must be standing in a Greenhouse plot.")
            return
        }
        val removed = activeLayoutMap()?.remove(plot.id) != null
        if (removed) {
            SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "clear-greenhouse-mutation-blueprint")
            resetMissingState()
            ChatUtils.chat("§aUnloaded the mutation layout from Greenhouse Plot §e${plot.id}§a.")
        } else {
            ChatUtils.chat("§eThere is no mutation layout loaded for this plot.")
        }
    }

    private fun setTarget(query: String) {
        val plot = currentGreenhousePlot() ?: run {
            ChatUtils.chat("§cYou must be standing in a Greenhouse plot.")
            return
        }
        val name = activeLayoutMap()?.get(plot.id)
        val blueprint = name?.let { layoutMap()?.get(it) } ?: run {
            ChatUtils.chat("§eThere is no mutation layout loaded for this plot.")
            return
        }
        val normalized = query.trim()
        val target = when {
            normalized.equals("none", ignoreCase = true) -> null
            normalized.equals("auto", ignoreCase = true) -> GreenhouseLayoutAnalysis.inferTarget(blueprint.entries())
            else -> GreenhouseMutation.fromQuery(normalized) ?: run {
                ChatUtils.chat("§cUnknown or ambiguous mutation: §e$normalized§c.")
                return
            }
        }
        blueprint.targetMutationId = when {
            target != null -> target.internalId
            normalized.equals("none", ignoreCase = true) -> NO_TARGET_MUTATION
            else -> ""
        }
        resetMissingState()
        SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "greenhouse-layout-target")
        if (target == null) {
            ChatUtils.chat(
                if (normalized.equals("auto", ignoreCase = true)) {
                    "§eCould not confidently detect a target. Use §b/shgreenhouseblueprint target <mutation>§e."
                } else {
                    "§aThis layout no longer ignores a spawned target mutation."
                },
            )
        } else {
            ChatUtils.chat(
                "§aSet §e${target.displayName} §aas the spawned output for §e$name§a. " +
                    "It will not create missing shadows when it moves or disappears.",
            )
        }
    }

    private fun showStatus() {
        val plot = currentGreenhousePlot() ?: run {
            ChatUtils.chat("§cYou must be standing in a Greenhouse plot.")
            return
        }
        val name = activeLayoutMap()?.get(plot.id)
        val blueprint = name?.let { layoutMap()?.get(it) } ?: run {
            ChatUtils.chat("§eThere is no mutation layout loaded for Greenhouse Plot §6${plot.id}§e.")
            return
        }
        ChatUtils.chat(
            "§aGreenhouse Plot §e${plot.id} §ais using §e$name §7(${blueprint.mutations.size} mutations)§a; " +
                "§c${missingPlacements.size + missingCropCells.size} §aentries are currently missing." +
                targetMessage(blueprint.targetMutation()),
        )
    }

    private fun greenhouseArea(plot: GardenPlot): AABB {
        val middle = plot.middle.toBlockPos()
        // The 10x10 planting grid consists of the inward-facing 5x5 corners of the
        // four central chunks in the 6x6-chunk plot.
        return AABB(
            (middle.x - GREENHOUSE_GRID_RADIUS).toDouble(),
            MIN_GARDEN_Y.toDouble(),
            (middle.z - GREENHOUSE_GRID_RADIUS).toDouble(),
            (middle.x + GREENHOUSE_GRID_RADIUS).toDouble(),
            MAX_GARDEN_Y.toDouble() + 1,
            (middle.z + GREENHOUSE_GRID_RADIUS).toDouble(),
        )
    }

    private fun AABB.isLoaded(): Boolean = listOf(
        LorenzVec(minX, MIN_GARDEN_Y.toDouble(), minZ),
        LorenzVec(minX, MIN_GARDEN_Y.toDouble(), maxZ - 1),
        LorenzVec(maxX - 1, MIN_GARDEN_Y.toDouble(), minZ),
        LorenzVec(maxX - 1, MIN_GARDEN_Y.toDouble(), maxZ - 1),
    ).all { it.isInLoadedChunk() }

    internal fun layouts(): Map<String, GreenHouseStorage.MutationBlueprintStorage> = layoutMap().orEmpty()

    internal fun targetMutation(blueprint: GreenHouseStorage.MutationBlueprintStorage): GreenhouseMutation? =
        blueprint.targetMutation()

    internal fun roleCounts(
        blueprint: GreenHouseStorage.MutationBlueprintStorage,
    ): Map<GreenhouseLayoutAnalysis.Role, Int> {
        val target = blueprint.targetMutation()
        return blueprint.entries().groupingBy { GreenhouseLayoutAnalysis.roleFor(it, target) }.eachCount()
    }

    internal fun activeLayoutName(plotId: Int): String? = activeLayoutMap()?.get(plotId)

    internal fun loadLayout(plotId: Int, name: String) {
        if (name !in layouts()) return
        activeLayoutMap()?.set(plotId, name)
        resetMissingState()
        SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "load-greenhouse-mutation-layout")
    }

    internal fun renameLayout(oldName: String, requestedName: String): Boolean {
        val newName = requestedName.trim().take(MAX_LAYOUT_NAME_LENGTH)
        if (newName.isEmpty()) {
            ChatUtils.chat("§cA Greenhouse layout name cannot be empty.")
            return false
        }
        val layouts = layoutMap() ?: run {
            ChatUtils.chat("§cYour SkyBlock profile storage is not available yet.")
            return false
        }
        val blueprint = layouts[oldName] ?: run {
            ChatUtils.chat("§cThe Greenhouse layout §e$oldName §cno longer exists.")
            return false
        }
        if (newName == oldName) return true
        if (newName in layouts) {
            ChatUtils.chat("§cA Greenhouse layout named §e$newName §calready exists.")
            return false
        }

        layouts.remove(oldName)
        layouts[newName] = blueprint
        activeLayoutMap()?.entries?.forEach { entry ->
            if (entry.value == oldName) entry.setValue(newName)
        }
        SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "rename-greenhouse-mutation-layout")
        ChatUtils.chat("§aRenamed Greenhouse layout §e$oldName §ato §e$newName§a.")
        return true
    }

    internal fun exportLayout(name: String) {
        val blueprint = layoutMap()?.get(name) ?: run {
            ChatUtils.chat("§cThe Greenhouse layout §e$name §cno longer exists.")
            return
        }
        val serialized = GreenhouseBlueprintFile.encode(name, blueprint)
        val exportDirectory = File(ConfigManager.configDirectory, "greenhouse-layouts").absoluteFile
        exportDirectory.mkdirs()
        val defaultFile = File(exportDirectory, GreenhouseBlueprintFile.suggestedFileName(name))
        DialogUtils.saveFileDialog("Export Greenhouse Layout", defaultFile.absolutePath) { selectedFile ->
            val file = File(GreenhouseBlueprintFile.withFileExtension(selectedFile.absolutePath))
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(serialized)
            }.onSuccess {
                ChatUtils.chat("§aExported Greenhouse layout §e$name §ato §b${file.absolutePath}§a.")
            }.onFailure { exception ->
                ChatUtils.chat("§cCould not export the Greenhouse layout: §7${exception.message}")
            }
        }
    }

    internal fun importLayoutFile(plotId: Int) {
        val importDirectory = File(ConfigManager.configDirectory, "greenhouse-layouts").absoluteFile
        importDirectory.mkdirs()
        DialogUtils.openFileDialog("Import Greenhouse Layout", importDirectory.absolutePath + File.separator) { file ->
            val imported = runCatching {
                require(file.isFile) { "The selected path is not a file." }
                require(file.length() <= GreenhouseBlueprintFile.MAX_FILE_BYTES) { "The layout file is too large." }
                GreenhouseBlueprintFile.decode(file.readText())
            }.getOrElse { exception ->
                ChatUtils.chat("§cCould not import the Greenhouse layout: §7${exception.message}")
                return@openFileDialog
            }
            val layouts = layoutMap() ?: run {
                ChatUtils.chat("§cYour SkyBlock profile storage is not available yet.")
                return@openFileDialog
            }
            val baseName = imported.name.take(MAX_LAYOUT_NAME_LENGTH).ifEmpty { file.nameWithoutExtension }
                .take(MAX_LAYOUT_NAME_LENGTH).ifEmpty { "Imported Layout" }
            val name = nextLayoutName(layouts.keys, baseName)
            layouts[name] = imported.blueprint
            activeLayoutMap()?.set(plotId, name)
            resetMissingState()
            SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "import-greenhouse-layout-file")
            ChatUtils.chat("§aImported and loaded Greenhouse layout §e$name§a.")
        }
    }

    internal fun deleteLayout(name: String) {
        if (layoutMap()?.remove(name) == null) return
        activeLayoutMap()?.entries?.removeIf { it.value == name }
        resetMissingState()
        SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "delete-greenhouse-mutation-layout")
    }

    private fun activeBlueprint(plot: GardenPlot): GreenHouseStorage.MutationBlueprintStorage? {
        val name = activeLayoutMap()?.get(plot.id) ?: return null
        return layoutMap()?.get(name)
    }

    private fun layoutMap(): MutableMap<String, GreenHouseStorage.MutationBlueprintStorage>? {
        val storage = greenhouseStorage ?: return null
        migrateLegacyBlueprints()
        return storage.mutationBlueprintLayouts ?: mutableMapOf<String, GreenHouseStorage.MutationBlueprintStorage>().also {
            storage.mutationBlueprintLayouts = it
        }
    }

    private fun activeLayoutMap(): MutableMap<Int, String>? {
        val storage = greenhouseStorage ?: return null
        migrateLegacyBlueprints()
        return storage.activeMutationBlueprintByPlot ?: mutableMapOf<Int, String>().also {
            storage.activeMutationBlueprintByPlot = it
        }
    }

    private fun migrateLegacyBlueprints() {
        val storage = greenhouseStorage ?: return
        val legacy = storage.mutationBlueprintsByPlot ?: return
        if (legacy.isEmpty()) return
        val layouts = storage.mutationBlueprintLayouts
            ?: mutableMapOf<String, GreenHouseStorage.MutationBlueprintStorage>().also {
                storage.mutationBlueprintLayouts = it
            }
        val active = storage.activeMutationBlueprintByPlot ?: mutableMapOf<Int, String>().also {
            storage.activeMutationBlueprintByPlot = it
        }
        legacy.forEach { (plotId, blueprint) ->
            blueprint.mutations.forEach { placement ->
                placement.offset = placement.offset.gridAnchor()
                placement.size = GreenhouseMutation.fromInternalId(placement.mutationId)?.size ?: placement.size
            }
            val name = nextLayoutName(layouts.keys, "Plot $plotId")
            layouts[name] = blueprint
            active[plotId] = name
        }
        legacy.clear()
    }

    private fun nextLayoutName(existing: Set<String>, base: String = "Layout"): String {
        if (base !in existing) return base
        var number = 2
        while ("$base $number" in existing) number++
        return "$base $number"
    }

    private fun currentGreenhousePlot(): GardenPlot? =
        GardenPlotApi.getCurrentPlot()?.takeIf { it.greenhouse && GreenhouseUtils.isInGreenhouse() }

    private fun LorenzVec.matchesAnchor(other: LorenzVec, size: Int): Boolean {
        val horizontalTolerance = if (size <= 1) ANCHOR_HORIZONTAL_TOLERANCE else size / 2.0 + 0.25
        return distanceSqIgnoreY(other) <= horizontalTolerance * horizontalTolerance &&
            kotlin.math.abs(y - other.y) <= ANCHOR_VERTICAL_TOLERANCE
    }

    private fun LorenzVec.gridAnchor(): LorenzVec = LorenzVec(floor(x), floor(y), floor(z))

    private fun GreenHouseStorage.BlueprintCellStorage.worldPosition(
        plot: GardenPlot,
        cropBlock: Boolean,
    ): LorenzVec {
        val middle = plot.middle.toBlockPos()
        return LorenzVec(
            middle.x + column - GREENHOUSE_GRID_RADIUS.toDouble(),
            if (cropBlock) IMPORTED_CROP_Y else IMPORTED_MUTATION_Y,
            middle.z + row - GREENHOUSE_GRID_RADIUS.toDouble(),
        )
    }

    private fun GreenHouseStorage.MutationPlacementStorage.worldPosition(
        plot: GardenPlot,
        usePlantingGridHeight: Boolean,
    ): LorenzVec {
        val savedPosition = plot.middle + offset
        return if (usePlantingGridHeight) {
            LorenzVec(savedPosition.x, IMPORTED_MUTATION_Y, savedPosition.z)
        } else {
            savedPosition
        }
    }

    private fun GreenHouseStorage.BlueprintCellStorage.cropCategory(): CropCategory? = when (cropId) {
        "wheat" -> CropCategory.WHEAT
        "potato" -> CropCategory.POTATO
        "carrot" -> CropCategory.CARROT
        "pumpkin" -> CropCategory.PUMPKIN
        "melon" -> CropCategory.MELON
        "cocoa_beans" -> CropCategory.COCOA_BEANS
        "sugar_cane" -> CropCategory.SUGAR_CANE
        "cactus" -> CropCategory.CACTUS
        "nether_wart" -> CropCategory.NETHER_WART
        "red_mushroom", "brown_mushroom" -> CropCategory.MUSHROOM
        "moonflower", "sunflower" -> CropCategory.SUNFLOWER
        "wild_rose" -> CropCategory.WILD_ROSE
        else -> null
    }

    private fun GreenHouseStorage.MutationBlueprintStorage.targetMutation(): GreenhouseMutation? = when {
        targetMutationId == NO_TARGET_MUTATION -> null
        targetMutationId.isNotEmpty() -> GreenhouseMutation.fromInternalId(targetMutationId)
        else -> GreenhouseLayoutAnalysis.inferTarget(entries())
    }

    private fun GreenHouseStorage.MutationBlueprintStorage.entries(): List<GreenhouseLayoutAnalysis.Entry> =
        if (importedCells.isNotEmpty()) {
            importedCells.map { cell ->
                val size = GreenhouseMutation.fromSkyShardsId(cell.cropId)?.size ?: 1
                GreenhouseLayoutAnalysis.Entry(cell.cropId, size * size, cell.target)
            }
        } else {
            mutations.map { placement ->
                val mutation = GreenhouseMutation.fromInternalId(placement.mutationId)
                val size = mutation?.size ?: placement.size
                GreenhouseLayoutAnalysis.Entry(placement.mutationId.lowercase(), size * size)
            }
        }

    private fun targetMessage(target: GreenhouseMutation?): String = if (target == null) {
        " §7No spawned output was detected; set one with §b/shgreenhouseblueprint target <mutation>§7."
    } else {
        " §7Spawned §e${target.displayName} §7outputs are ignored."
    }

    private fun resetMissingState() {
        previousMissing = null
        stableMissingChecks = 0
        missingPlacements = emptySet()
        missingCropCells = emptySet()
    }

    private fun resetRuntimeState() {
        activePlotId = null
        previousMissing = null
        stableMissingChecks = 0
        missingPlacements = emptySet()
        missingCropCells = emptySet()
    }

    private data class MissingState(
        val mutations: Set<Int>,
        val crops: Set<Int>,
    )

    private const val MIN_GARDEN_Y = 60
    private const val MAX_GARDEN_Y = 100
    private const val GREENHOUSE_GRID_RADIUS = 5
    private const val REQUIRED_STABLE_CHECKS = 3
    private const val ANCHOR_HORIZONTAL_TOLERANCE = 0.1
    private const val ANCHOR_VERTICAL_TOLERANCE = 3.0
    internal const val MAX_LAYOUT_NAME_LENGTH = 32
    private const val IMPORTED_MUTATION_Y = 74.0
    private const val IMPORTED_CROP_Y = 74.0
    private const val NO_TARGET_MUTATION = "NONE"
}
