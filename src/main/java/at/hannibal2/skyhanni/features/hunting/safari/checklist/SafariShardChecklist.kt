package at.hannibal2.skyhanni.features.hunting.safari.checklist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.foraging.SafariChecklistConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.hunting.SafariRunStartEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.LocationUtils.playerLocation
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object SafariShardChecklist {

    private val checklistConfig get() = SkyHanniMod.feature.hunting.safari.checklist

    private val patternGroup = RepoPattern.group("hunting.safari.run.shard-tracker")

    /**
     * REGEX-TEST: LOOT SHARE! You received 2x Parakeet Shard from meowgirlemily catching a Parakeet!
     * REGEX-TEST: LOOT SHARE! You received an Areita Shard from VirulentNyx catching an Areita!
     * REGEX-TEST: LOOT SHARE! You received a Hideyho Shard from TCrest finding the Hideyho!
     */
    private val lootShareShardPattern by patternGroup.pattern(
        "loot-share",
        """LOOT SHARE! You received (?:an?|(?<amount>\d+)x) (?<shardName>.+) Shards? from .*!""",
    )

    /**
     * REGEX-TEST: CAPTURE! You caught a Strongarm and gained a Strongarm Shard!
     * REGEX-TEST: CAPTURE! You caught a Solsnatcher and gained 2x Solsnatcher Shard!
     * REGEX-TEST: CAPTURE! You found Hideyho, and as a reward he gave you 4x Hideyho Shard!
     * REGEX-TEST: CAPTURE! You found the Hideyho, and as a reward it gave you a Hideyho Shard!
     */
    @Suppress("MaxLineLength")
    private val capturedShardPattern by patternGroup.pattern(
        "capture",
        """CAPTURE! You (?:(?:caught an?|found) .+ and (?:gained|as a reward (?:he|she|they) gave you)|found (?:the )?Hideyho, and as a reward it gave you) (?:an?|(?<amount>\d+)x) (?<shardName>.+) Shard!""",
    )

    private val shardCounts = SafariShard.entries.associateWithTo(mutableMapOf()) { 0 }

    val collectedShards: Map<SafariShard, Int> get() = shardCounts

    @HandleEvent
    private fun onSafariRunStart(event: SafariRunStartEvent) {
        reset()
    }

    internal fun reset() {
        shardCounts.clear()
        SafariShard.entries.forEach { shardCounts[it] = 0 }
    }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        processChatMessage(event.cleanMessage)
    }


    internal fun processChatMessage(message: String) {
        lootShareShardPattern.matchMatcher(message) {
            addShard(group("shardName"), groupOrNull("amount")?.toInt() ?: 1)
        }

        capturedShardPattern.matchMatcher(message) {
            addShard(group("shardName"), groupOrNull("amount")?.toInt() ?: 1)
        }
    }

    private fun addShard(name: String, amount: Int) {
        val shard = SafariShard.getByName(name) ?: return
        shardCounts[shard] = shardCounts.getValue(shard) + amount
    }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onRender(event: GuiRenderEvent.GuiOnTopRenderEvent) {
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
