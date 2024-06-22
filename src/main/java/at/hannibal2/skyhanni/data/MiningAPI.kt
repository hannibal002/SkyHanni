package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ColdUpdateEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.mining.OreMinedEvent
import at.hannibal2.skyhanni.events.player.PlayerDeathEvent
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.features.mining.OreBlock
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.countBy
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matchFirst
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MiningAPI {

    private val group = RepoPattern.group("data.miningapi")
    private val glaciteAreaPattern by group.pattern("area.glacite", "Glacite Tunnels|Glacite Lake")
    private val dwarvenBaseCampPattern by group.pattern("area.basecamp", "Dwarven Base Camp")
    val coldReset by group.pattern(
        "cold.reset",
        "§6The warmth of the campfire reduced your §r§b❄ Cold §r§6to §r§a0§r§6!|§c ☠ §r§7You froze to death§r§7.",
    )
    private val coldResetDeath by group.pattern(
        "cold.deathreset",
        "§c ☠ §r§7§r§.(?<name>.+)§r§7 (?<reason>.+)",
    )

    data class MinedBlock(val ore: OreBlock, var position: LorenzVec, var confirmed: Boolean, val time: SimpleTimeMark)

    private var lastInitSound = SimpleTimeMark.farPast()

    private var waitingForInitBlock = false
    private var waitingForInitBlockPos: LorenzVec? = null
    private var waitingForInitSound = true

    private var waitingForEffMinerSound = false
    private var waitingForEffMinerBlock = false

    var inGlacite = false
    var inDwarvenMines = false
    var inCrystalHollows = false
    var inCrimsonIsle = false
    var inEnd = false
    var inSpidersDen = false

    var currentAreaOreBlocks = setOf<OreBlock>()

    private var lastSkyblockArea: String? = null

    private var recentClickedBlocks = mutableListOf<MinedBlock>()
    private var surroundingMinedBlocks = mutableListOf<MinedBlock>()
    private val allowedSoundNames = listOf("dig.glass", "dig.stone", "dig.gravel", "dig.cloth")

    var cold: Int = 0
        private set

    var lastColdUpdate = SimpleTimeMark.farPast()
    var lastColdReset = SimpleTimeMark.farPast()

    fun inGlaciteArea() = inGlacialTunnels() || IslandType.MINESHAFT.isInIsland()

    fun inDwarvenBaseCamp() = IslandType.DWARVEN_MINES.isInIsland() && dwarvenBaseCampPattern.matches(LorenzUtils.skyBlockArea)

    fun inRegularDwarven() = IslandType.DWARVEN_MINES.isInIsland() && !inGlacialTunnels()

    fun inCrystalHollows() = IslandType.CRYSTAL_HOLLOWS.isInIsland()

    fun inMineshaft() = IslandType.MINESHAFT.isInIsland()

    fun inGlacialTunnels() = IslandType.DWARVEN_MINES.isInIsland() && glaciteAreaPattern.matches(LorenzUtils.skyBlockArea)

    fun inCustomMiningIsland() = inAnyIsland(
        IslandType.DWARVEN_MINES,
        IslandType.MINESHAFT,
        IslandType.CRYSTAL_HOLLOWS,
        IslandType.THE_END,
        IslandType.CRIMSON_ISLE,
        IslandType.SPIDER_DEN,
    )

    fun inColdIsland() = inAnyIsland(IslandType.DWARVEN_MINES, IslandType.MINESHAFT)

    @SubscribeEvent
    fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        val newCold = event.scoreboard.matchFirst(ScoreboardPattern.coldPattern) {
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
        val blockState = event.getBlockState
        val ore = OreBlock.getByStateOrNull(blockState) ?: return
        recentClickedBlocks.add(MinedBlock(ore, position, false, SimpleTimeMark.now()))
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
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (event.name == LorenzUtils.getPlayerName()) {
            updateCold(0)
            lastColdReset = SimpleTimeMark.now()
        }
    }

    @SubscribeEvent
    fun onPlaySound(event: PlaySoundEvent) {
        if (!inCustomMiningIsland()) return
        if (waitingForInitSound) {
            if (event.soundName in allowedSoundNames && event.pitch == 0.7936508f) {
                val pos = event.location.roundLocationToBlock()
                if (recentClickedBlocks.none { it.position == pos }) return
                waitingForInitSound = false
                waitingForInitBlock = true
                waitingForInitBlockPos = event.location.roundLocationToBlock()
                lastInitSound = SimpleTimeMark.now()
            }
            return
        }
        if (waitingForEffMinerSound) {
            if (surroundingMinedBlocks.isEmpty()) return
            if (event.soundName in allowedSoundNames || event.soundName == "random.orb") {
                if (surroundingMinedBlocks.last().confirmed) return
                waitingForEffMinerSound = false
                surroundingMinedBlocks.last().confirmed = true
                waitingForEffMinerBlock = true
            }
        }
    }

    @SubscribeEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!inCustomMiningIsland()) return
        if (event.newState.block != Blocks.air) return
        if (event.oldState.block == Blocks.air) return
        if (event.location.distanceToPlayer() > 7) return

        if (lastInitSound.passedSince() > 100.milliseconds) return

        val ore = OreBlock.getByStateOrNull(event.oldState) ?: return

        if (waitingForInitBlock) {
            if (waitingForInitBlockPos != event.location) return
            waitingForInitBlock = false
            surroundingMinedBlocks.add(MinedBlock(ore, event.location, true, SimpleTimeMark.now()))
            waitingForEffMinerBlock = true
            return
        }
        if (waitingForEffMinerBlock) {
            if (surroundingMinedBlocks.any { it.position == event.location }) return
            waitingForEffMinerBlock = false
            surroundingMinedBlocks.add(MinedBlock(ore, event.location, false, SimpleTimeMark.now()))
            waitingForEffMinerSound = true
            return
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!inCustomMiningIsland()) return

        if (LorenzUtils.lastWorldSwitch.passedSince() < 4.seconds) return
        updateLocation()

        if (currentAreaOreBlocks.isEmpty()) return

        // if somehow you take more than 20 seconds to mine a single block, congrats
        recentClickedBlocks.removeIf { it.time.passedSince() > 20.seconds }
        surroundingMinedBlocks.removeIf { it.time.passedSince() > 20.seconds }

        if (surroundingMinedBlocks.isEmpty()) return
        if (lastInitSound.passedSince() < 200.milliseconds) return

        resetOreEvent()

        val originalBlock = surroundingMinedBlocks.firstOrNull { it.confirmed } ?: run {
            surroundingMinedBlocks = mutableListOf()
            recentClickedBlocks = mutableListOf()
            return
        }

        val extraBlocks = surroundingMinedBlocks.filter { it.confirmed }.countBy { it.ore }

        OreMinedEvent(originalBlock.ore, extraBlocks).post()

        surroundingMinedBlocks = mutableListOf()
        recentClickedBlocks.removeIf { it.time.passedSince() >= originalBlock.time.passedSince() }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (cold != 0) updateCold(0)
        lastColdReset = SimpleTimeMark.now()
        recentClickedBlocks = mutableListOf()
        surroundingMinedBlocks = mutableListOf()
        currentAreaOreBlocks = setOf()
        resetOreEvent()
    }

    private fun resetOreEvent() {
        lastInitSound = SimpleTimeMark.farPast()
        waitingForInitSound = true
        waitingForInitBlock = false
        waitingForInitBlockPos = null
        waitingForEffMinerSound = false
        waitingForEffMinerBlock = false
    }

    @SubscribeEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Mining API")
        if (!inCustomMiningIsland()) {
            event.addIrrelevant("not in a mining island")
            return
        }

        event.addData {
            if (lastInitSound.isFarPast()) {
                add("lastInitSound: never")
            } else {
                add("lastInitSound: ${lastInitSound.passedSince().format()}")
            }
            add("waitingForInitSound: $waitingForInitSound")
            add("waitingForInitBlock: $waitingForInitBlock")
            add("waitingForInitBlockPos: $waitingForInitBlockPos")
            add("waitingForEffMinerSound: $waitingForEffMinerSound")
            add("waitingForEffMinerBlock: $waitingForEffMinerBlock")
            add("recentClickedBlocks: ${recentClickedBlocks.joinToString { it.position.toCleanString() }}")
        }
    }

    private fun updateCold(newCold: Int) {
        // Hypixel sends cold data once in scoreboard even after resetting it
        if (cold == 0 && lastColdUpdate.passedSince() < 1.seconds) return
        lastColdUpdate = SimpleTimeMark.now()
        ColdUpdateEvent(newCold).postAndCatch()
        cold = newCold
    }

    private fun updateLocation() {
        val currentArea = LorenzUtils.skyBlockArea
        // TODO add area change event with HypixelData.skyBlockArea instead
        if (currentArea == lastSkyblockArea) return
        lastSkyblockArea = currentArea

        inGlacite = inGlaciteArea()
        inDwarvenMines = inRegularDwarven()
        inCrystalHollows = inCrystalHollows()
        inCrimsonIsle = IslandType.CRIMSON_ISLE.isInIsland()
        inEnd = IslandType.THE_END.isInIsland()
        inSpidersDen = IslandType.SPIDER_DEN.isInIsland()

        currentAreaOreBlocks = OreBlock.entries.filter { it.checkArea() }.toSet()
    }
}
