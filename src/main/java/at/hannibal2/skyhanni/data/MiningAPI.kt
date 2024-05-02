package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ColdUpdateEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ScoreboardChangeEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.mining.CustomBlockMineEvent
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.features.mining.OreBlock
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchFirst
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object MiningAPI {

    private val group = RepoPattern.group("data.miningapi")
    private val glaciteAreaPattern by group.pattern("area.glacite", "Glacite Tunnels|Glacite Lake")
    val coldReset by group.pattern(
        "cold.reset",
        "§6The warmth of the campfire reduced your §r§b❄ Cold §r§6to §r§a0§r§6!|§c ☠ §r§7You froze to death§r§7."
    )
    val coldResetDeath by group.pattern(
        "cold.deathreset",
        "§c ☠ §r§7§r§.(?<name>.+)§r§7 (?<reason>.+)"
    )

    class MinedBlock(val ore: OreBlock, var position: LorenzVec, var confirmed: Boolean, val time: SimpleTimeMark)

    var isBeingMined = false
    var lastSound = SimpleTimeMark.farPast()

    private var recentMinedBlocksMap = mutableListOf<MinedBlock>()
    private var surroundingMinedBlocks = mutableListOf<MinedBlock>()
    private val allowedSoundNames = listOf("dig.glass", "dig.stone", "dig.gravel", "dig.cloth")

    private var cold = 0
    var lastColdUpdate = SimpleTimeMark.farPast()
    var lastColdReset = SimpleTimeMark.farPast()


    fun inGlaciteArea() = inGlacialTunnels() || IslandType.MINESHAFT.isInIsland()

    fun inRegularDwarven() = IslandType.DWARVEN_MINES.isInIsland() && !inGlacialTunnels()

    fun inCrystalHollows() = IslandType.CRYSTAL_HOLLOWS.isInIsland()

    fun inMineshaft() = IslandType.MINESHAFT.isInIsland()

    fun inGlacialTunnels() =
        IslandType.DWARVEN_MINES.isInIsland() && glaciteAreaPattern.matches(LorenzUtils.skyBlockArea)

    fun inCustomMiningIsland() = inAnyIsland(
        IslandType.DWARVEN_MINES, IslandType.MINESHAFT, IslandType.CRYSTAL_HOLLOWS,
        IslandType.THE_END, IslandType.CRIMSON_ISLE, IslandType.SPIDER_DEN
    )

    fun inColdIsland() = inAnyIsland(IslandType.DWARVEN_MINES, IslandType.MINESHAFT)

    fun getCold() = cold

    @SubscribeEvent
    fun onScoreboardChange(event: ScoreboardChangeEvent) {
        val newCold = event.newList.matchFirst(ScoreboardPattern.coldPattern) {
            group("cold").toInt().absoluteValue
        } ?: return

        if (newCold != cold) {
            updateCold(newCold)
        }
    }

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!inCustomMiningIsland()) return
        if (event.clickType != ClickType.LEFT_CLICK) return
        val position = event.position
        if (recentMinedBlocksMap.any { it.position == position }) return
        val blockState = event.getBlockState
        val ore = OreBlock.getByStateOrNull(blockState) ?: return
        recentMinedBlocksMap.add(MinedBlock(ore, position, false, SimpleTimeMark.now()))
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!inColdIsland()) return
        if (coldReset.matches(event.message)) {
            updateCold(0)
            lastColdReset = SimpleTimeMark.now()
        }
        coldResetDeath.matchMatcher(event.message) {
            if (group("name") == LorenzUtils.getPlayerName()) {
                updateCold(0)
                lastColdReset = SimpleTimeMark.now()
            }
        }
    }

    @SubscribeEvent
    fun onPlaySound(event: PlaySoundEvent) {
        if (!inCustomMiningIsland()) return
        if (allowedSoundNames.none { it == event.soundName } && event.soundName != "random.orb") return
        if (!isBeingMined && event.pitch == 0.7936508f) {
            if (allowedSoundNames.none { it == event.soundName }) return
            recentMinedBlocksMap.firstOrNull { it.position == event.location - LorenzVec(0.5, 0.5, 0.5) }?.confirmed =
                true
            isBeingMined = true
            lastSound = SimpleTimeMark.now()
            return
        } else if (isBeingMined && event.soundName == "random.orb" && event.volume == 0.5f) {
            if (lastSound.passedSince() > 100.milliseconds) return
            if (!surroundingMinedBlocks.last().confirmed) {
                surroundingMinedBlocks.last().confirmed = true
                lastSound = SimpleTimeMark.now()
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!inCustomMiningIsland()) return

        // if somehow you take more than 20 seconds to mine a single block, congrats
        recentMinedBlocksMap.removeIf { it.time.passedSince() > 20.seconds }
        surroundingMinedBlocks.removeIf { it.time.passedSince() > 20.seconds }

        if (lastSound.passedSince() < 200.milliseconds) return
        isBeingMined = false
        val originalBlock = recentMinedBlocksMap.firstOrNull { it.confirmed } ?: return

        val blocksMinedMap = surroundingMinedBlocks
            .filter { it.confirmed }
            .groupBy({ it.ore }, { 1 })
            .mapValues { it.value.size }

        CustomBlockMineEvent(originalBlock.ore, blocksMinedMap).postAndCatch()

        surroundingMinedBlocks = mutableListOf()
        recentMinedBlocksMap.removeIf { it.time.passedSince() >= originalBlock.time.passedSince() }
        lastSound = SimpleTimeMark.farPast()
    }

    @SubscribeEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!inCustomMiningIsland()) return
        if (event.newState.block != Blocks.air) return
        if (event.location.distanceToPlayer() > 7) return
        val ore = OreBlock.getByStateOrNull(event.oldState) ?: return
        surroundingMinedBlocks.add(MinedBlock(ore, event.location, false, SimpleTimeMark.now()))
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (cold != 0) updateCold(0)
        lastColdReset = SimpleTimeMark.now()
        recentMinedBlocksMap = mutableListOf()
        surroundingMinedBlocks = mutableListOf()
    }

    private fun updateCold(newCold: Int) {
        // Hypixel sends cold data once in scoreboard even after resetting it
        if (cold == 0 && lastColdUpdate.passedSince() < 1.seconds) return
        lastColdUpdate = SimpleTimeMark.now()
        ColdUpdateEvent(newCold).postAndCatch()
        cold = newCold
    }
}
