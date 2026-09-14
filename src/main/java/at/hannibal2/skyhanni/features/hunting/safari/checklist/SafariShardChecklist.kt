package at.hannibal2.skyhanni.features.hunting.safari.checklist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.foraging.SafariChecklistConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.item.ShardGainEvent
import at.hannibal2.skyhanni.events.item.ShardSource
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LocationUtils.playerLocation
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object SafariShardChecklist {

    private val checklistConfig get() = SkyHanniMod.feature.hunting.safari.checklist

    private val shardCounts = SafariShard.entries.associateWithTo(mutableMapOf()) { 0 }

    private var display = emptyList<Renderable>()
    private var lastBiome: SafariBiome? = null

    @HandleEvent(priority = HandleEvent.LOWEST)
    private fun onRepoReload(event: RepositoryReloadEvent) {
        DelayedRun.runOrNextTick {
            SafariShard.entries.forEach { it.resetCache() }
            updateDisplay()
        }
    }

    @HandleEvent(priority = HandleEvent.LOWEST)
    private fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        DelayedRun.runOrNextTick {
            SafariShard.entries.forEach { it.resetCache() }
            updateDisplay()
        }
    }

    @HandleEvent
    private fun onWorldSwap(event: WorldChangeEvent) {
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
    private fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(
            checklistConfig.runShardChecklistDisplay,
            checklistConfig.hideCollectedRunShards,
            checklistConfig.showShardIcons,
        ) {
            updateDisplay()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onTick(event: SkyHanniTickEvent) {
        val biome = getCurrentBiome()
        if (biome == lastBiome) return
        lastBiome = biome
        updateDisplay()
    }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onGuiRenderTop(event: GuiRenderEvent.GuiOnTopRenderEvent) {
        if (!checklistConfig.runShardChecklist) return
        checklistConfig.runShardChecklistPosition.renderRenderables(display, posLabel = "Safari Shard Checklist")
    }

    internal fun reset() {
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
        val biomes = when (checklistConfig.runShardChecklistDisplay.get()) {
            SafariChecklistConfig.ChecklistDisplay.ALL -> SafariBiome.entries
            SafariChecklistConfig.ChecklistDisplay.CURRENT_ON_TOP -> getCurrentBiome()?.let { currentBiome ->
                SafariBiome.entries.sortedByDescending { it == currentBiome }
            } ?: SafariBiome.entries

            SafariChecklistConfig.ChecklistDisplay.ONLY_CURRENT -> getCurrentBiome()?.let { listOf(it) }.orEmpty()
        }
        biomes.forEach { biome ->
            val status = if (isBiomeDone(biome)) "§aDone" else "§cUndone"
            add(Renderable.text("${biome.formattedName} §7- $status"))
            biome.shards.filter { !checklistConfig.hideCollectedRunShards.get() || shardCounts.getValue(it) == 0 }.forEach { shard ->
                val marker = if (shardCounts.getValue(shard) > 0) "§a✔" else "§c✖"
                val row = buildList {
                    add(Renderable.text("§7- ($marker§7)"))
                    if (checklistConfig.showShardIcons.get()) {
                        shard.itemStack?.let { add(Renderable.item(it)) }
                    }
                    add(Renderable.text(formatShardName(shard.displayName, shard.rarity)))
                }
                add(Renderable.horizontal(row, spacing = 2))
            }
        }
        if (checklistConfig.runShardChecklistDisplay.get() == SafariChecklistConfig.ChecklistDisplay.ONLY_CURRENT) {
            SafariBiome.entries.filter { it !in biomes }.forEach { biome ->
                val collected = biome.shards.count { shardCounts.getValue(it) > 0 }
                val status = if (isBiomeDone(biome)) "§aDone" else "§7$collected/${biome.shards.size}"
                add(Renderable.text("${biome.formattedName} §7- $status"))
            }
        }
    }

    private fun updateDisplay() {
        display = createDisplay()
    }

    internal fun isBiomeDone(biome: SafariBiome): Boolean = biome.shards.all { shardCounts.getValue(it) > 0 }

    internal fun formatShardName(displayName: String, rarity: LorenzRarity?): String =
        "${rarity?.chatColorCode ?: "§f"}$displayName"

    internal fun shardNameToRarity(shardName: String): LorenzRarity? =
        SafariShard.getByName(shardName)?.rarity

    internal fun shardInternalNameToRarity(internalName: NeuInternalName): LorenzRarity? =
        SafariShard.entries.firstOrNull { it.internalName == internalName }?.rarity

    internal fun getCurrentBiome(): SafariBiome? {
        val playerLocation = playerLocation()
        val centerX = -49.5
        val centerZ = 0.5
        return when {
            playerLocation.x < centerX && playerLocation.z < centerZ -> SafariBiome.ICY
            playerLocation.x < centerX && playerLocation.z >= centerZ -> SafariBiome.CAVERN
            playerLocation.x >= centerX && playerLocation.z >= centerZ -> SafariBiome.FOREST
            playerLocation.x >= centerX && playerLocation.z < centerZ -> SafariBiome.HAUNTED
            else -> null
        }
    }
}
