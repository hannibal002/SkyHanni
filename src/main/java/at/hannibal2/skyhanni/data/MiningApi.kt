package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.MiningJson
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ColdUpdateEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.mining.OreMinedEvent
import at.hannibal2.skyhanni.events.player.PlayerDeathEvent
import at.hannibal2.skyhanni.events.skyblock.ScoreboardAreaChangeEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi.dungeonRoomPattern
import at.hannibal2.skyhanni.features.mining.OreBlock
import at.hannibal2.skyhanni.features.mining.isTitanium
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.countBy
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.netty.util.internal.ConcurrentSet
import net.minecraft.init.Blocks
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Suppress("MemberVisibilityCanBePrivate")
@SkyHanniModule
object MiningApi {

    private val group = RepoPattern.group("data.miningapi")

    /**
     * REGEX-TEST: Glacite Tunnels
     * REGEX-TEST: Great Glacite Lake
     */
    private val glaciteAreaPattern by group.pattern("area.glacite", "Glacite Tunnels|Great Glacite Lake")
    private val dwarvenBaseCampPattern by group.pattern("area.basecamp", "Dwarven Base Camp")

    /**
     * REGEX-TEST: Mines of Divan
     */
    private val minesOfDivanPattern by group.pattern("area.minesofdivan", "Mines of Divan")

    /**
     * REGEX-TEST: §6The warmth of the campfire reduced your §r§b❄ Cold §r§6to §r§a0§r§6!
     * REGEX-TEST: §c ☠ §r§7You froze to death§r§7.
     */
    private val coldResetPattern by group.pattern(
        "cold.reset",
        "§6The warmth of the campfire reduced your §r§b❄ Cold §r§6to §r§a0§r§6!|§c ☠ §r§7You froze to death§r§7\\.",
    )

    /**
     * REGEX-TEST: Heat: §6IMMUNE
     * REGEX-TEST: Heat: §c14♨
     * REGEX-TEST: Heat: §c0♨
     */
    val heatPattern by group.pattern(
        "heat.scoreboard",
        "^Heat: (?<scoreboard>§.(?<heat>\\d+|IMMUNE)♨?)\$",
    )

    /**
     * REGEX-TEST: Cold: §b-1❄
     */
    val coldPattern by group.pattern(
        "cold",
        "(?:§.)*Cold: §.(?<cold>-?\\d+)❄",
    )

    private val pickbobulusGroup = group.group("pickobulus")

    /**
     * REGEX-TEST: §aYou used your §r§6Pickobulus §r§aPickaxe Ability!
     */

    private val pickobulusUsePattern by pickbobulusGroup.pattern(
        "use",
        "§aYou used your §r§6Pickobulus §r§aPickaxe Ability!",
    )

    /**
     * REGEX-TEST: §7Your §r§aPickobulus §r§7destroyed §r§e140 §r§7blocks!
     */
    private val pickobulusEndPattern by pickbobulusGroup.pattern(
        "end",
        "§7Your §r§aPickobulus §r§7destroyed §r§e(?<amount>[\\d,.]+) §r§7blocks!",
    )

    /**
     * REGEX-TEST: §7Your §r§aPickobulus §r§7didn't destroy any blocks!
     */
    private val pickobulusFailPattern by pickbobulusGroup.pattern(
        "fail",
        "§7Your §r§aPickobulus §r§7didn't destroy any blocks!",
    )

    private data class MinedBlock(val ore: OreBlock, var confirmed: Boolean) {
        val time: SimpleTimeMark = SimpleTimeMark.now()
    }

    // normal mining
    private val recentClickedBlocks = ConcurrentSet<Pair<LorenzVec, SimpleTimeMark>>()
    private val surroundingMinedBlocks = ConcurrentLinkedQueue<Pair<MinedBlock, LorenzVec>>()

    private var lastClickedPos: LorenzVec? = null
    private var lastClicked = SimpleTimeMark.farPast()
    private var ignoreInit = false

    private var lastInitSound = SimpleTimeMark.farPast()

    private var initBlockPos: LorenzVec? = null
    private var waitingForInitSound = true

    private var waitingForEffMinerSound = false
    private var waitingForEffMinerBlock = false

    // pickobulus
    private var lastPickobulusUse = SimpleTimeMark.farPast()
    private var lastPickobulusExplosion = SimpleTimeMark.farPast()
    private var pickobulusExplosionPos: LorenzVec? = null
    private val pickobulusMinedBlocks = ConcurrentLinkedQueue<Pair<LorenzVec, OreBlock>>()

    private val pickobulusActive get() = lastPickobulusUse.passedSince() < 2.seconds

    private var pickobulusWaitingForSound = false
    private var pickobulusWaitingForBlock = false

    // oreblock data
    var inGlacite = false
        private set
    var inTunnels = false
        private set
    var inMineshaft = false
        private set
    var inDwarvenMines = false
        private set
    var inCrystalHollows = false
        private set
    var inCrimsonIsle = false
        private set
    var inEnd = false
        private set
    var inSpidersDen = false
        private set

    var currentAreaOreBlocks = setOf<OreBlock>()
        private set

    val blockStrengths = mutableMapOf<OreBlock, Int>()

    private val allowedSoundNames = setOf(
        "dig.glass", "dig.stone", "dig.gravel", "dig.cloth", "random.orb",
        //#if MC > 1.21
        //$$ "block.metal.place",
        //#endif
    )

    var heat: Int = 0
        private set
    var heatDisplay: String? = null
        private set
    var lastHeatUpdate = SimpleTimeMark.farPast()
        private set

    var cold: Int = 0
        private set

    var mineshaftRoomId: String? = null
        private set

    var lastColdUpdate = SimpleTimeMark.farPast()
        private set
    var lastColdReset = SimpleTimeMark.farPast()
        private set

    private var lastOreMinedTime = SimpleTimeMark.farPast()

    fun inGlaciteArea() = inGlacialTunnels() || IslandType.MINESHAFT.isCurrent()

    fun inDwarvenBaseCamp() = IslandType.DWARVEN_MINES.isCurrent() && dwarvenBaseCampPattern.matches(SkyBlockUtils.graphArea)

    fun inRegularDwarven() = IslandType.DWARVEN_MINES.isCurrent() && !inGlacialTunnels()

    fun inCrystalHollows() = IslandType.CRYSTAL_HOLLOWS.isCurrent()

    fun inMinesOfDivan() = inCrystalHollows() && minesOfDivanPattern.matches(HypixelData.skyBlockArea)

    fun inMineshaft() = IslandType.MINESHAFT.isCurrent()

    fun inGlacialTunnels() = IslandType.DWARVEN_MINES.isCurrent() && glaciteAreaPattern.matches(SkyBlockUtils.graphArea)

    @HandleEvent
    fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        if (IslandTypeTags.IS_COLD.inAny()) {
            dungeonRoomPattern.firstMatcher(event.new) {
                groupOrNull("roomId")?.let { mineshaftRoomId = it }
            }

            coldPattern.firstMatcher(event.added) {
                val newCold = group("cold").toInt().absoluteValue

                if (newCold != cold) {
                    updateCold(newCold)
                }
            }
        }

        if (IslandType.CRYSTAL_HOLLOWS.isCurrent()) {
            var found = false
            heatPattern.firstMatcher(event.new) {
                found = true
                val newHeat = group("heat")
                heatDisplay = group("scoreboard").takeIf { it.isNotEmpty() }
                if (newHeat == "IMMUNE") {
                    updateHeat(0)
                } else if (newHeat.toInt() != heat) {
                    updateHeat(newHeat.toInt())
                }
            }
            if (!found) {
                if (heat != 0) {
                    updateHeat(0)
                }
                heatDisplay = null
            }
        }
    }

    @HandleEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!IslandTypeTags.CUSTOM_MINING.inAny()) return
        if (event.clickType != ClickType.LEFT_CLICK) return
        if (OreBlock.getByStateOrNull(event.getBlockState) == null) return
        val now = SimpleTimeMark.now()
        recentClickedBlocks += event.position to now
        lastClickedPos = event.position
        lastClicked = now
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!IslandTypeTags.CUSTOM_MINING.inAny()) return
        if (IslandTypeTags.IS_COLD.inAny()) {
            if (coldResetPattern.matches(event.message)) {
                updateCold(0)
                lastColdReset = SimpleTimeMark.now()
                return
            }
        }
        if (pickobulusUsePattern.matches(event.message)) {
            lastPickobulusUse = SimpleTimeMark.now()
            return
        }
        if (pickobulusFailPattern.matches(event.message)) {
            resetPickobulusEvent()
            pickobulusMinedBlocks.clear()
            return
        }
        pickobulusEndPattern.matchMatcher(event.message) {
            val amount = group("amount").formatInt()
            resetPickobulusEvent()
            val blocks = pickobulusMinedBlocks.take(amount).countBy { it.second }
            if (blocks.isNotEmpty()) OreMinedEvent(null, blocks).post()
            pickobulusMinedBlocks.clear()
            return
        }

    }

    @HandleEvent
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (event.name == PlayerUtils.getName()) {
            updateCold(0)
            updateHeat(0)
            lastColdReset = SimpleTimeMark.now()
            lastHeatUpdate = SimpleTimeMark.now()
        }
    }

    @HandleEvent
    fun onPlaySound(event: PlaySoundEvent) {
        if (!IslandTypeTags.CUSTOM_MINING.inAny()) return
        if (event.soundName == "random.explode" && lastPickobulusUse.passedSince() < 5.seconds) {
            lastPickobulusExplosion = SimpleTimeMark.now()
            pickobulusExplosionPos = event.location
            pickobulusWaitingForSound = true
            return
        }
        if (event.soundName !in allowedSoundNames) return
        if (pickobulusActive && pickobulusWaitingForSound) {
            pickobulusWaitingForSound = false
            pickobulusWaitingForBlock = true
            return
        }
        if (waitingForInitSound) {
            if (event.soundName != "random.orb") {
                if (event.pitch != 0.7936508f) return
                val pos = event.location.roundToBlock()
                if (recentClickedBlocks.none { it.first == pos }) return
                waitingForInitSound = false
                waitingForEffMinerBlock = true
                initBlockPos = event.location.roundToBlock()
                lastInitSound = SimpleTimeMark.now()
            } else {
                if (lastClicked.passedSince() > 1.seconds) return
                val block = lastClickedPos ?: return
                val ore = OreBlock.getByStateOrNull(block.getBlockStateAt()) ?: return
                if (ore.hasInitSound) return
                ignoreInit = true
                waitingForInitSound = false
                waitingForEffMinerBlock = true
                lastInitSound = SimpleTimeMark.now()
            }
        }
        if (waitingForEffMinerSound) {
            val lastBlock = surroundingMinedBlocks.lastOrNull()?.first ?: return
            if (lastBlock.confirmed) return
            waitingForEffMinerSound = false
            lastBlock.confirmed = true
            waitingForEffMinerBlock = true
        }
    }

    @HandleEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!IslandTypeTags.CUSTOM_MINING.inAny()) return
        val oldState = event.oldState
        val newState = event.newState
        val oldBlock = oldState.block
        val newBlock = newState.block

        if (oldState == newState) return
        if (oldBlock == Blocks.air || oldBlock == Blocks.bedrock) return
        if (newBlock != Blocks.air && newBlock != Blocks.bedrock && !isTitanium(newState)) return

        val pos = event.location
        if (pickobulusActive && pickobulusWaitingForBlock) {
            val explosionPos = pickobulusExplosionPos ?: return
            if (explosionPos.distance(pos) > 15) return
            val ore = OreBlock.getByStateOrNull(oldState) ?: return
            if (pickobulusMinedBlocks.any { it.first == pos }) return
            pickobulusMinedBlocks += pos to ore
            pickobulusWaitingForBlock = false
            pickobulusWaitingForSound = true
            return
        }

        if (lastInitSound.passedSince() > 100.milliseconds) return
        if (pos.distanceToPlayer() > 7) return

        val ore = OreBlock.getByStateOrNull(oldState) ?: return

        if (initBlockPos == pos) {
            surroundingMinedBlocks += MinedBlock(ore, true) to pos
            runEvent()
            return
        }

        if (waitingForEffMinerBlock && (!ignoreInit || !ore.hasInitSound)) {
            if (surroundingMinedBlocks.any { it.second == pos }) return
            waitingForEffMinerBlock = false
            surroundingMinedBlocks += MinedBlock(ore, false) to pos
            waitingForEffMinerSound = true
            return
        }
    }

    @HandleEvent
    fun onTick() {
        if (!IslandTypeTags.CUSTOM_MINING.inAny()) return
        if (currentAreaOreBlocks.isEmpty()) return

        // if somehow you take more than 10 seconds to mine a single block, congrats
        recentClickedBlocks.removeIf { it.second.passedSince() >= 10.seconds }
        surroundingMinedBlocks.removeIf { it.first.time.passedSince() >= 5.seconds }

        if (!waitingForInitSound && lastInitSound.passedSince() > 200.milliseconds) {
            if (ignoreInit) runEvent()
            else resetOreEvent()
        }
        if (!lastPickobulusUse.isFarPast() && lastPickobulusUse.passedSince() > 5.seconds) {
            resetPickobulusEvent()
            pickobulusMinedBlocks.clear()
        }
    }

    @HandleEvent(ScoreboardAreaChangeEvent::class)
    fun onAreaChange() {
        if (!IslandTypeTags.CUSTOM_MINING.inAny()) return
        updateLocation()
    }

    @HandleEvent
    fun onIslandChange() {
        updateLocation()

        mineshaftRoomId = null
    }

    private fun runEvent() {
        val ignoreFilter = ignoreInit
        resetOreEvent()

        if (surroundingMinedBlocks.isEmpty()) return

        val originalBlock = surroundingMinedBlocks.firstOrNull { it.first.confirmed }?.first ?: run {
            surroundingMinedBlocks.clear()
            recentClickedBlocks.clear()
            return
        }

        val extraBlocks = surroundingMinedBlocks.filter {
            // We can do this because all blocks that don't have an init sound also cannot be mined by
            // efficient miner when other blocks are mined.
            // The more correct way of doing this would be making sure the oretype of the originally mined
            // block matches
            if (ignoreFilter) it.first.ore == originalBlock.ore else it.first.confirmed
        }.countBy { it.first.ore }

        OreMinedEvent(originalBlock.ore, extraBlocks).post()
        lastOreMinedTime = SimpleTimeMark.now()

        surroundingMinedBlocks.clear()
        recentClickedBlocks.removeIf { it.second.passedSince() >= originalBlock.time.passedSince() }
        lastClickedPos = null
    }

    @HandleEvent
    fun onWorldChange() {
        if (cold != 0) updateCold(0)
        lastColdReset = SimpleTimeMark.now()
        recentClickedBlocks.clear()
        surroundingMinedBlocks.clear()
        lastClickedPos = null
        pickobulusMinedBlocks.clear()
        currentAreaOreBlocks = setOf()
        resetOreEvent()
        resetPickobulusEvent()
        lastOreMinedTime = SimpleTimeMark.farPast()
        inDwarvenMines = false
        inCrystalHollows = false
        inGlacite = false
    }

    private fun resetOreEvent() {
        lastInitSound = SimpleTimeMark.farPast()
        waitingForInitSound = true
        ignoreInit = false
        initBlockPos = null
        waitingForEffMinerSound = false
        waitingForEffMinerBlock = false
    }

    private fun resetPickobulusEvent() {
        lastPickobulusUse = SimpleTimeMark.farPast()
        lastPickobulusExplosion = SimpleTimeMark.farPast()
        pickobulusExplosionPos = null
        pickobulusWaitingForSound = false
        pickobulusWaitingForBlock = false
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Mining API")
        if (!IslandTypeTags.CUSTOM_MINING.inAny()) {
            event.addIrrelevant("not in a mining island")
            return
        }
        if (lastOreMinedTime.passedSince() > 30.seconds) {
            event.addIrrelevant("not mined recently")
            return
        }

        fun SimpleTimeMark.formatTime(): String {
            if (isFarPast()) return "never"
            return passedSince().format()
        }

        event.addData {
            add("lastClickedPos: ${lastClickedPos?.toCleanString()}")
            add("lastClicked: ${lastClicked.formatTime()}")
            add("ignoreInit: $ignoreInit")
            add("lastInitSound: ${lastInitSound.formatTime()}")
            add("initBlockPos: ${initBlockPos?.toCleanString()}")
            add("waitingForInitSound: $waitingForInitSound")
            add("waitingForEffMinerSound: $waitingForEffMinerSound")
            add("waitingForEffMinerBlock: $waitingForEffMinerBlock")
            add("")
            add("lastPickobulusUse: ${lastPickobulusUse.formatTime()}")
            add("lastPickobulusExplosion: ${lastPickobulusExplosion.formatTime()}")
            add("pickobulusExplosionPos: ${pickobulusExplosionPos?.toCleanString()}")
            add("pickobulusWaitingForSound: $pickobulusWaitingForSound")
            add("pickobulusWaitingForBlock: $pickobulusWaitingForBlock")
            add("")
            add("recentlyClickedBlocks: ${recentClickedBlocks.joinToString { "(${it.first.toCleanString()}" }}")
        }
    }

    private fun updateCold(newCold: Int) {
        // Hypixel sends cold data once in scoreboard even after resetting it
        if (cold == 0 && lastColdUpdate.passedSince() < 1.seconds) return
        lastColdUpdate = SimpleTimeMark.now()
        ColdUpdateEvent(newCold).post()
        cold = newCold
    }

    private fun updateHeat(newHeat: Int) {
        if (heat == 0 && lastHeatUpdate.passedSince() < 1.seconds) return
        lastHeatUpdate = SimpleTimeMark.now()
        heat = newHeat
    }

    private fun updateLocation() {
        inGlacite = inGlaciteArea()
        inTunnels = inGlacialTunnels()
        inMineshaft = inMineshaft()
        inDwarvenMines = inRegularDwarven()
        inCrystalHollows = inCrystalHollows()
        inCrimsonIsle = IslandType.CRIMSON_ISLE.isCurrent()
        inEnd = IslandType.THE_END.isCurrent()
        inSpidersDen = IslandType.SPIDER_DEN.isCurrent()

        currentAreaOreBlocks = OreBlock.entries.filter { it.checkArea() }.toSet()
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val repo = event.getConstant<MiningJson>("Mining")

        blockStrengths.clear()
        repo.blockStrengths.forEach { (key, value) ->
            val ore = OreBlock.getByNameOrNull(key) ?: return@forEach
            blockStrengths[ore] = value
        }
    }
}
