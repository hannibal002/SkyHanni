package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.MiningJson
import at.hannibal2.skyhanni.data.model.SkyblockStat
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
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.events.skyblock.ScoreboardAreaChangeEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.mining.OreBlock
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.countBy
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import java.util.concurrent.ConcurrentHashMap
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

    private val icyBiomePattern by group.pattern("area.icybiome", "Icy Biome")

    /**
     * REGEX-TEST: §6The warmth of the campfire reduced your §r§b Cold §r§6to §r§a0§r§6!
     * REGEX-TEST: §c ☠ §r§7You froze to death§r§7.
     */
    @Suppress("MaxLineLength")
    private val coldResetPattern by group.pattern(
        "cold.reset",
        "§6The warmth of the campfire reduced your §r§b${SkyblockStat.COLD_RESISTANCE.hypixelIcon} Cold §r§6to §r§a0§r§6!|§c ☠ §r§7You froze to death§r§7\\.",
    )

    // This intentionally uses the old heat icon, since Hypixel has not updated it in this location.
    /**
     * REGEX-TEST: Heat: §6IMMUNE
     * REGEX-TEST: Heat: §c14♨
     * REGEX-TEST: Heat: §c0♨
     */
    val heatPattern by group.pattern(
        "heat.scoreboard",
        "^Heat: (?<scoreboard>§.(?<heat>\\d+|IMMUNE)♨?)\$",
    )

    // This intentionally uses the old cold icon, since Hypixel has not updated it in this location.
    /**
     * REGEX-TEST: Cold: §b-1❄
     * REGEX-TEST: Cold: §b-3❄
     */
    val coldPattern by group.pattern(
        "cold",
        "(?:§.)*Cold: §.(?<cold>-?\\d+)❄",
    )

    private val pickobulusGroup = group.group("pickobulus")

    /**
     * REGEX-TEST: §aYou used your §r§6Pickobulus §r§aPickaxe Ability!
     */

    private val pickobulusUsePattern by pickobulusGroup.pattern(
        "use",
        "§aYou used your §r§6Pickobulus §r§aPickaxe Ability!",
    )

    /**
     * REGEX-TEST: §7Your §r§aPickobulus §r§7destroyed §r§e140 §r§7blocks!
     */
    private val pickobulusEndPattern by pickobulusGroup.pattern(
        "end",
        "§7Your §r§aPickobulus §r§7destroyed §r§e(?<amount>[\\d,.]+) §r§7blocks!",
    )

    /**
     * REGEX-TEST: §7Your §r§aPickobulus §r§7didn't destroy any blocks!
     */
    private val pickobulusFailPattern by pickobulusGroup.pattern(
        "fail",
        "§7Your §r§aPickobulus §r§7didn't destroy any blocks!",
    )

    private data class MinedBlock(val ore: OreBlock, var confirmed: Boolean) {
        val time: SimpleTimeMark = SimpleTimeMark.now()
    }

    // normal mining
    private val recentClickedBlocks = ConcurrentHashMap<LorenzVec, SimpleTimeMark>()
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

    // OreBlock data
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
        "block.glass.break",
        "block.stone.break",
        "block.gravel.break",
        "block.wool.break",
        "entity.experience_orb.pickup",
        "block.metal.place",
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

    fun inGlaciteArea() = inGlacialTunnels() || IslandType.MINESHAFT.isInIsland()

    fun inDwarvenBaseCamp() = IslandType.DWARVEN_MINES.isInIsland() && dwarvenBaseCampPattern.matches(SkyBlockUtils.graphArea)

    fun inRegularDwarven() = IslandType.DWARVEN_MINES.isInIsland() && !inGlacialTunnels()

    fun inCrystalHollows() = IslandType.CRYSTAL_HOLLOWS.isInIsland()

    fun inMinesOfDivan() = inCrystalHollows() && minesOfDivanPattern.matches(HypixelData.skyBlockArea)

    fun inMineshaft() = IslandType.MINESHAFT.isInIsland()

    fun inGlacialTunnels() = IslandType.DWARVEN_MINES.isInIsland() && glaciteAreaPattern.matches(SkyBlockUtils.graphArea)

    /**
     * Whether cold can currently apply to the player.
     * On the Critter Safari cold is limited to the Icy Biome, which is why the area is checked here.
     * The other cold islands are not narrowed down further, there we rely on Hypixel removing the scoreboard line.
     */
    fun inColdArea(): Boolean {
        if (!IslandTypeTag.IS_COLD.isInIsland()) return false
        if (IslandType.SAFARI.isInIsland()) return icyBiomePattern.matches(SkyBlockUtils.graphArea)
        return true
    }

    @HandleEvent
    private fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        if (IslandTypeTag.IS_COLD.isInIsland()) {
            DungeonApi.dungeonRoomPattern.firstMatcher(event.new) {
                groupOrNull("roomId")?.let { mineshaftRoomId = it }
            }

            var found = false
            if (inColdArea()) {
                coldPattern.firstMatcher(event.new) {
                    found = true
                    val newCold = group("cold").toInt().absoluteValue

                    if (newCold != cold) {
                        updateCold(newCold)
                    }
                }
            }
            // Cold is reset when the line is gone from the scoreboard, and also when the player left the cold area,
            // since Hypixel only removes the line after a delay.
            if (!found) resetCold()
        }

        if (IslandType.CRYSTAL_HOLLOWS.isInIsland()) {
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
    private fun onBlockClick(event: BlockClickEvent) {
        if (!IslandTypeTag.CUSTOM_MINING.isInIsland()) return
        if (event.clickType != InteractClickType.LEFT_CLICK) return
        if (OreBlock.getByStateOrNull(event.blockState) == null) return
        val now = SimpleTimeMark.now()
        recentClickedBlocks[event.position] = now
        lastClickedPos = event.position
        lastClicked = now
    }

    @HandleEvent
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!IslandTypeTag.CUSTOM_MINING.isInIsland()) return
        if (IslandTypeTag.IS_COLD.isInIsland()) {
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
    private fun onPlayerDeath(event: PlayerDeathEvent.Allow) {
        if (event.isSelf) {
            updateCold(0)
            updateHeat(0)
            lastColdReset = SimpleTimeMark.now()
            lastHeatUpdate = SimpleTimeMark.now()
        }
    }

    @HandleEvent
    private fun onPlaySound(event: PlaySoundEvent) {
        if (!IslandTypeTag.CUSTOM_MINING.isInIsland()) return
        if (event.soundName == "entity.generic.explode" && lastPickobulusUse.passedSince() < 5.seconds) {
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
            if (if (event.soundName == "entity.experience_orb.pickup") orbSound() else event.noOrbSound()) {
                return
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

    private fun orbSound(): Boolean {
        if (lastClicked.passedSince() > 1.seconds) return true
        val block = lastClickedPos ?: return true
        val ore = OreBlock.getByStateOrNull(block.getBlockStateAt()) ?: return true
        if (ore.hasInitSound) return true
        ignoreInit = true
        waitingForInitSound = false
        waitingForEffMinerBlock = true
        lastInitSound = SimpleTimeMark.now()
        return false
    }

    private fun PlaySoundEvent.noOrbSound(): Boolean {
        if (pitch != 0.7936508f) return true
        val pos = location.roundToBlock()
        if (!recentClickedBlocks.containsKey(pos)) return true
        waitingForInitSound = false
        waitingForEffMinerBlock = true
        initBlockPos = location.roundToBlock()
        lastInitSound = SimpleTimeMark.now()
        return false
    }

    @HandleEvent
    private fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!IslandTypeTag.CUSTOM_MINING.isInIsland()) return
        val oldState = event.oldState
        val newState = event.newState
        val oldBlock = oldState.block
        val newBlock = newState.block

        if (oldState == newState) return
        if (oldBlock == Blocks.AIR || oldBlock == Blocks.BEDROCK) return
        if (newBlock != Blocks.AIR && newBlock != Blocks.BEDROCK && !OreBlock.isTitanium(newState)) return

        handleBlockBreak(event.location, oldState)
    }

    private fun handleBlockBreak(pos: LorenzVec, oldState: BlockState) {
        if (tryHandlePickobulusBlock(pos, oldState)) return

        if (lastInitSound.passedSince() > 100.milliseconds) return
        if (pos.distanceToPlayer() > 7) return

        val ore = OreBlock.getByStateOrNull(oldState) ?: return

        if (initBlockPos == pos) {
            surroundingMinedBlocks += MinedBlock(ore, true) to pos
            runEvent()
            return
        }

        handleEffMinerBlock(ore, pos)
    }

    private fun handleEffMinerBlock(ore: OreBlock, pos: LorenzVec) {
        if (!waitingForEffMinerBlock) return
        if (ignoreInit && ore.hasInitSound) return

        if (surroundingMinedBlocks.any { it.second == pos }) return
        waitingForEffMinerBlock = false
        surroundingMinedBlocks += MinedBlock(ore, false) to pos
        waitingForEffMinerSound = true
        return
    }

    private fun tryHandlePickobulusBlock(pos: LorenzVec, oldState: BlockState): Boolean {
        if (!pickobulusActive || !pickobulusWaitingForBlock) return false
        val explosionPos = pickobulusExplosionPos ?: return true
        if (explosionPos.distance(pos) > 15) return true
        val ore = OreBlock.getByStateOrNull(oldState) ?: return true
        if (pickobulusMinedBlocks.any { it.first == pos }) return true
        pickobulusMinedBlocks += pos to ore
        pickobulusWaitingForBlock = false
        pickobulusWaitingForSound = true
        return true
    }

    @HandleEvent
    private fun onTick() {
        if (!IslandTypeTag.CUSTOM_MINING.isInIsland()) return
        if (currentAreaOreBlocks.isEmpty()) return

        // if somehow you take more than 10 seconds to mine a single block, congrats
        recentClickedBlocks.removeIf { it.value.passedSince() >= 10.seconds }
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
    private fun onScoreboardAreaChange() {
        if (!IslandTypeTag.CUSTOM_MINING.isInIsland()) return
        updateLocation()
    }

    // Resets cold the moment the player leaves a cold area, without waiting for the scoreboard to catch up.
    @HandleEvent(GraphAreaChangeEvent::class)
    private fun onAreaChange() {
        if (!inColdArea()) resetCold()
    }

    @HandleEvent
    private fun onIslandChange() {
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
            // The more correct way of doing this would be making sure the OreType of the originally mined
            // block matches
            if (ignoreFilter) it.first.ore == originalBlock.ore else it.first.confirmed
        }.countBy { it.first.ore }

        OreMinedEvent(originalBlock.ore, extraBlocks).post()
        lastOreMinedTime = SimpleTimeMark.now()

        surroundingMinedBlocks.clear()
        recentClickedBlocks.removeIf { it.value.passedSince() >= originalBlock.time.passedSince() }
        lastClickedPos = null
    }

    @HandleEvent
    private fun onWorldChange() {
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
    private fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Mining API")
        if (!IslandTypeTag.CUSTOM_MINING.isInIsland()) {
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
            add("recentlyClickedBlocks: ${recentClickedBlocks.keys.joinToString { "(${it.toCleanString()})" }}")
        }
    }

    private fun resetCold() {
        if (cold == 0) return
        updateCold(0)
        lastColdReset = SimpleTimeMark.now()
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
        inCrimsonIsle = IslandType.CRIMSON_ISLE.isInIsland()
        inEnd = IslandType.THE_END.isInIsland()
        inSpidersDen = IslandType.SPIDER_DEN.isInIsland()

        currentAreaOreBlocks = OreBlock.entries.filter { it.checkArea() }.toSet()
    }

    @HandleEvent
    private fun onRepoReload(event: RepositoryReloadEvent) {
        val repo = event.getConstant<MiningJson>("Mining")

        blockStrengths.clear()
        repo.blockStrengths.forEach { (key, value) ->
            OreBlock.getByNameOrNull(key)?.let { ore ->
                blockStrengths[ore] = value
            }
        }
    }
}
