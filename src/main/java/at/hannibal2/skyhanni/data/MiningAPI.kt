package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ColdUpdateEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ScoreboardChangeEvent
import at.hannibal2.skyhanni.events.mining.CompactUpdateEvent
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.features.mining.OreBlock
import at.hannibal2.skyhanni.features.mining.OreType
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchFirst
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.truncate
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object MiningAPI {

    private val group = RepoPattern.group("data.miningapi")
    private val glaciteAreaPattern by group.pattern("area.glacite", "Glacite Tunnels")
    val coldReset by group.pattern(
        "cold.reset",
        "§cThe warmth of the campfire reduced your §r§b❄ Cold §r§cto 0!|§c ☠ §r§7You froze to death§r§7."
    )

    class MinedBlock(val ore: OreBlock, var position: LorenzVec, val time: SimpleTimeMark)
    class Sound(
        val soundName: String,
        val location: LorenzVec,
        val pitch: Float,
        val volume: Float,
        val time: SimpleTimeMark
    )

    private var recentMinedBlocksMap = mutableListOf<MinedBlock>()
    private var soundsList = mutableListOf<Sound>()
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
        recentMinedBlocksMap.add(MinedBlock(ore, position, SimpleTimeMark.now()))
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!inColdIsland()) return
        if (coldReset.matches(event.message)) {
            updateCold(0)
            lastColdReset = SimpleTimeMark.now()
        }
    }

    @SubscribeEvent
    fun onPlaySound(event: PlaySoundEvent) {
        if (!inCustomMiningIsland()) return
        val position = event.location - LorenzVec(0.5, 0.5, 0.5)
        if (soundsList.isEmpty() && event.pitch == 0.7936508f) {
            if (allowedSoundNames.none { it == event.soundName }) return
            soundsList.add(Sound(event.soundName, position, event.pitch, event.volume, SimpleTimeMark.now()))
        } else if (soundsList.isNotEmpty() && event.soundName == "random.orb" && event.volume == 0.5f) {
            if (soundsList.last().time.passedSince() > 100.milliseconds) return
            soundsList.add(Sound(event.soundName, position, event.pitch, event.volume, SimpleTimeMark.now()))
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!inCustomMiningIsland()) return

        recentMinedBlocksMap.removeIf { it.time.passedSince() > 20.seconds }
        soundsList.removeIf { it.time.passedSince() > 500.milliseconds }

        val firstSound = soundsList.firstOrNull() ?: return

        if (soundsList.last().time.passedSince() > 200.milliseconds) {
            val blocks = soundsList.count { it.soundName == "random.orb" }
            val block = recentMinedBlocksMap.firstOrNull { it.position == firstSound.location } ?: return

            recentMinedBlocksMap.removeIf { it.time < block.time }
            soundsList.clear()

            if (blocks == 0) return
            val adjacentHardstone = block.position.getAdjacentHardstone()
            val adjacentBedrock = block.position.getAdjacentBedrock()
            println("bedrock: $adjacentBedrock")
            if (hasEpicOrAboveScathaPet() && adjacentHardstone > 0 && block.ore.oreType != OreType.HARD_STONE) {
                val molePerk = getMinMaxHardstoneMole()
                val totalPair = Pair(adjacentHardstone + molePerk.first, adjacentHardstone + molePerk.second)
                val maxEfficientMiner = geMaxBlocksEfficientMiner() + 1
                if (maxEfficientMiner + totalPair.second == blocks) {
                    CompactUpdateEvent(blocks - totalPair.second, block.ore)
                    CompactUpdateEvent(totalPair.second, OreBlock.HARD_STONE_GLACIAL)
                } else if (totalPair.first == totalPair.second || molePerk.third < 0.5) {
                    CompactUpdateEvent(blocks - totalPair.first, block.ore)
                    CompactUpdateEvent(totalPair.first, OreBlock.HARD_STONE_GLACIAL)
                } else {
                    CompactUpdateEvent(blocks - totalPair.second, block.ore)
                    CompactUpdateEvent(totalPair.second, OreBlock.HARD_STONE_GLACIAL)
                }
            } else {
                CompactUpdateEvent(blocks, block.ore).postAndCatch()
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (cold != 0) updateCold(0)
        lastColdReset = SimpleTimeMark.now()
        recentMinedBlocksMap = mutableListOf()
        soundsList = mutableListOf()
    }

    private fun updateCold(newCold: Int) {
        // Hypixel sends cold data once in scoreboard even after resetting it
        if (cold == 0 && lastColdUpdate.passedSince() < 1.seconds) return
        lastColdUpdate = SimpleTimeMark.now()
        ColdUpdateEvent(newCold).postAndCatch()
        cold = newCold
    }

    private fun hasEpicOrAboveScathaPet(): Boolean {
        val pet = PetAPI.currentPet ?: return false
        return (!pet.startsWith("§9") && pet.contains("Scatha"))
    }

    private fun getMinMaxHardstoneMole(): Triple<Int, Int, Double> {
        if (!HotmData.MOLE.enabled) return Triple(0, 0, 0.0)
        val value = HotmData.MOLE.getReward().getValue(HotmReward.AVERAGE_BLOCK_BREAKS)
        return Triple(ceil(value).toInt(), truncate(value).toInt(), value % 1)
    }

    private fun geMaxBlocksEfficientMiner(): Int {
        if (!HotmData.EFFICIENT_MINER.enabled) return 0
        val level = HotmData.EFFICIENT_MINER.activeLevel
        return ceil((0.5 * level) + 0.5).toInt()
    }

    private fun LorenzVec.getAdjacentHardstone(): Int {
        var count = 0
        EnumFacing.entries.forEach {
            val position = this.toBlockPos().offset(it).toLorenzVec()
            val ore = OreBlock.getByStateOrNull(position.getBlockStateAt())?.oreType
            if (ore == OreType.HARD_STONE) ++count
        }
        return count.coerceIn(0..2)
    }

    private fun LorenzVec.getAdjacentBedrock(): Int {
        var count = 0
        EnumFacing.entries.forEach {
            val position = this.toBlockPos().offset(it).toLorenzVec()
            val block = position.getBlockAt()
            if (block == Blocks.bedrock) ++count
        }
        return count.coerceIn(0..2)
    }
}
