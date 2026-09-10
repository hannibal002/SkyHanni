package at.hannibal2.skyhanni.features.hunting.safari.checklist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.foraging.SafariChecklistConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.item.ShardGainEvent
import at.hannibal2.skyhanni.events.item.ShardSource
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
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

    @HandleEvent
    private fun onWorldSwap(event: WorldChangeEvent) {
        reset()
    }

    internal fun reset() {
        shardCounts.clear()
        SafariShard.entries.forEach { shardCounts[it] = 0 }
    }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onShardGain(event: ShardGainEvent) {
        if (event.source != ShardSource.HUNT && event.source != ShardSource.CAPTURED) return
        val shard = SafariShard.entries.firstOrNull { it.internalName == event.shardInternalName } ?: return
        addShard(shard.displayName, event.amount)
    }

    private fun addShard(name: String, amount: Int) {
        val shard = SafariShard.getByName(name) ?: return
        shardCounts[shard] = shardCounts.getValue(shard) + amount
    }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onGuiRenderTop(event: GuiRenderEvent.GuiOnTopRenderEvent) {
        if (!checklistConfig.runShardChecklist) return
        checklistConfig.runShardChecklistPosition.renderRenderables(createDisplay(), posLabel = "Safari Shard Checklist")
    }

    private fun createDisplay(): List<Renderable> = buildList {
        val biomes = when (checklistConfig.runShardChecklistDisplay) {
            SafariChecklistConfig.ChecklistDisplay.ALL -> SafariBiome.entries
            SafariChecklistConfig.ChecklistDisplay.CURRENT_ON_TOP -> getCurrentBiome()?.let { currentBiome ->
                SafariBiome.entries.sortedByDescending { it == currentBiome }
            } ?: SafariBiome.entries

            SafariChecklistConfig.ChecklistDisplay.ONLY_CURRENT -> getCurrentBiome()?.let { listOf(it) }.orEmpty()
        }
        biomes.forEach { biome ->
            val status = if (isBiomeDone(biome)) "§aDone" else "§cUndone"
            add(Renderable.text("${biome.formattedName} §7- $status"))
            biome.shards.filter { !checklistConfig.hideCollectedRunShards || shardCounts.getValue(it) == 0 }.forEach { shard ->
                val marker = if (shardCounts.getValue(shard) > 0) "§a✔" else "§c✖"
                val row = buildList {
                    add(Renderable.text("§7- ($marker§7)"))
                    shard.itemStack?.let { add(Renderable.item(it)) }
                    add(Renderable.text(formatShardName(shard.displayName, shard.rarity)))
                }
                add(Renderable.horizontal(row, spacing = 2))
            }
        }
    }

    internal fun isBiomeDone(biome: SafariBiome): Boolean = biome.shards.all { shardCounts.getValue(it) > 0 }

    internal fun formatShardName(displayName: String, rarity: LorenzRarity?): String =
        "${rarity?.chatColorCode ?: "§f"}$displayName"

    internal fun shardNameToRarity(shardName: String): LorenzRarity? =
        SafariShard.getByName(shardName)?.itemStack?.getItemRarityOrNull()

    internal fun shardInternalNameToRarity(internalName: NeuInternalName): LorenzRarity? =
        SafariShard.entries.firstOrNull { it.internalName == internalName }?.let { shardNameToRarity(it.displayName) }

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
