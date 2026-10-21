package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.hunting.safari.SafariChecklistConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.item.ShardGainEvent
import at.hannibal2.skyhanni.events.item.ShardSource
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object SafariShardChecklist {

    private val config get() = SkyHanniMod.feature.hunting.safari.checklist

    private val shardCounts = SafariShard.entries.associateWithTo(mutableMapOf()) { 0 }

    private var display = emptyList<Renderable>()
    private var lastBiome: SafariBiome? = null

    @HandleEvent(priority = HandleEvent.LOWEST)
    private fun onRepoReload() {
        DelayedRun.runOrNextTick {
            SafariShard.entries.forEach { it.resetCache() }
            updateDisplay()
        }
    }

    @HandleEvent(priority = HandleEvent.LOWEST)
    private fun onNeuRepoReload() {
        DelayedRun.runOrNextTick {
            SafariShard.entries.forEach { it.resetCache() }
            updateDisplay()
        }
    }

    @HandleEvent
    private fun onWorldChange() {
        reset()
    }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onShardGain(event: ShardGainEvent) {
        if (event.source != ShardSource.HUNT && event.source != ShardSource.CAPTURED) return
        val shard = SafariShard.entries.firstOrNull { it.internalName == event.shardInternalName } ?: return
        addShard(shard.displayName, event.amount)
        updateDisplay()
    }

    @HandleEvent
    private fun onConfigLoad() {
        ConditionalUtils.onToggle(
            config.runDisplay,
            config.hideCaught,
            config.showIcons,
        ) {
            updateDisplay()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onTick() {
        val biome = SafariBiome.currentArea()
        if (biome == lastBiome) return
        lastBiome = biome
        updateDisplay()
    }

    @HandleEvent
    private fun onIslandJoin() {
        updateDisplay()
    }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onGuiRenderTop() {
        if (!config.enabled) return
        config.position.renderRenderables(display, posLabel = "Safari Shard Checklist")
    }

    private fun reset() {
        shardCounts.clear()
        SafariShard.entries.forEach { shardCounts[it] = 0 }
        lastBiome = null
        updateDisplay()
    }

    private fun addShard(name: String, amount: Int) {
        val shard = SafariShard.getByName(name) ?: return
        shardCounts[shard] = shardCounts.getValue(shard) + amount
    }

    private fun createDisplay(): List<Renderable> = buildList {
        val currentArea = SafariBiome.currentArea()
        val detailedBiomes = when (config.runDisplay.get()) {
            SafariChecklistConfig.ChecklistDisplay.ALL -> SafariBiome.entries
            SafariChecklistConfig.ChecklistDisplay.CURRENT_ON_TOP -> SafariBiome.entries.sortedByDescending { it == currentArea }
            SafariChecklistConfig.ChecklistDisplay.ONLY_CURRENT -> currentArea?.let { listOf(it) }.orEmpty()
        }

        detailedBiomes.forEach { addBiomeDetails(it) }

        if (config.runDisplay.get() == SafariChecklistConfig.ChecklistDisplay.ONLY_CURRENT) {
            SafariBiome.entries.filter { it !in detailedBiomes }.forEach { biome ->
                addBiomeHeader(biome)
            }
        }
    }

    private fun MutableList<Renderable>.addBiomeHeader(biome: SafariBiome) {
        val collected = biome.shards.count { shardCounts.getValue(it) > 0 }
        val color = if (collected == biome.shards.size) "§a" else "§c"
        val status = "$color$collected/${biome.shards.size}"
        add(Renderable.text("${biome.formattedName} §7- $status"))
    }

    private fun MutableList<Renderable>.addBiomeDetails(biome: SafariBiome) {
        addBiomeHeader(biome)
        biome.shards.filter { !config.hideCaught.get() || shardCounts.getValue(it) == 0 }.forEach { shard ->
            val marker = if (shardCounts.getValue(shard) > 0) "§a✔" else "§c✖"
            val row = buildList {
                add(Renderable.text("§7- ($marker§7)"))
                if (config.showIcons.get()) {
                    shard.itemStack?.let { add(Renderable.item(it)) }
                }
                add(Renderable.text(shard.formattedName))
            }
            add(Renderable.horizontal(row, spacing = 2))
        }
    }

    private fun updateDisplay() {
        if (!IslandType.SAFARI.isInIsland()) {
            display = emptyList()
            return
        }
        display = createDisplay()
    }
}
