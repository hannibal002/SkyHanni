package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.mining.nucleus.CrystalNucleusSpeedrunConfig.RunSplitType
import at.hannibal2.skyhanni.config.features.mining.nucleus.CrystalNucleusSpeedrunConfig.RunStartType
import at.hannibal2.skyhanni.config.features.mining.nucleus.CrystalNucleusSpeedrunConfig.RunStopType
import at.hannibal2.skyhanni.config.storage.ResettableStorageSet
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.SkyHanniWarpEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.mining.CrystalNucleusCrystalFoundEvent
import at.hannibal2.skyhanni.events.mining.CrystalNucleusCrystalPlacedEvent
import at.hannibal2.skyhanni.events.mining.CrystalNucleusLootEvent
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.NucleusCrystalType
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusSpeedrunTracker.Data.SpeedrunState
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusSpeedrunTracker.Data.SpeedrunTransitionType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.farPast
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import net.minecraft.client.entity.EntityPlayerSP
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CrystalNucleusSpeedrunTracker {

    /**
     * This whole module is essentially an overcomplicated finite state machine, mixed in
     * with some of the logic from the chat filter, and a SkyHanniTracker for session data.
     * Data storage happens synchronously based on the state machine itself.
     */

    private val tracker = SkyHanniTracker(
        "Crystal Nucleus Speedrun Tracker",
        { Data() },
        { it.mining.crystalNucleusSpeedrunTracker },
    ) { drawDisplay(it) }

    class Data : TrackerData() {
        @Expose var runsCompleted: Int = 0
        @Expose var goldSplits: MutableMap<NucleusCrystalType, Duration> = mutableMapOf()
        @Expose var splitsCache: MutableMap<NucleusCrystalType, MutableList<Duration>> = mutableMapOf()
        @Expose var bestRun: CrystalNucleusSpeedrun = CrystalNucleusSpeedrun()
        @Expose var currentRun: CrystalNucleusSpeedrun = CrystalNucleusSpeedrun()

        fun cacheSplits(splitMap: MutableMap<NucleusCrystalType, Duration>) {
            splitMap.forEach { (crystalType, duration) ->
                val cachedKeyedSplits = splitsCache.getOrPut(crystalType) { mutableListOf() }
                if (cachedKeyedSplits.size < runsCompleted) cachedKeyedSplits.add(duration)
                else {
                    cachedKeyedSplits.removeFirst()
                    cachedKeyedSplits.add(duration)
                }

                // Check against gold splits
                val cachedGoldSplit = goldSplits[crystalType]
                if (cachedGoldSplit == null || duration < cachedGoldSplit) {
                    goldSplits[crystalType] = duration
                }
            }
        }

        override fun reset() {
            runsCompleted = 0
            goldSplits.clear()
            splitsCache.clear()
            bestRun = CrystalNucleusSpeedrun()
            currentRun = CrystalNucleusSpeedrun()
        }

        enum class SpeedrunState(private val displayName: String) {
            NOT_STARTED("Not Started"),
            IN_PROGRESS("In Progress"),
            SPLITTING("Splitting"),
            IDLE_WAITING("Idle (Waiting)"),
            ;

            override fun toString() = displayName
        }

        data class CrystalNucleusSpeedrun(
            var state: SpeedrunState = SpeedrunState.NOT_STARTED,
            var inProgressCrystal: NucleusCrystalType? = null,
            var splits: MutableMap<NucleusCrystalType, Duration> = mutableMapOf(),
            var startedAt: SimpleTimeMark = farPast(),
            var finishedAt: SimpleTimeMark = farPast(),
            var unattributedTime: Duration = 0.milliseconds,
        ) : ResettableStorageSet() {
            val currentlyCollectedCrystals get() = splits.keys.toList()

            fun isInitialized() = startedAt != farPast() && inProgressCrystal != null
            fun init(nucleusCrystalType: NucleusCrystalType) {
                reset()
                startedAt = SimpleTimeMark.now()
                inProgressCrystal = nucleusCrystalType
            }

            /**
             * Returns true if the current run is better than the other run based on the sum of the split times.
             */
            fun isBetterThan(other: CrystalNucleusSpeedrun): Boolean {
                val thisSplits = splits.values.sumOf { it.inWholeMilliseconds }
                val otherSplits = other.splits.values.sumOf { it.inWholeMilliseconds }
                return thisSplits < otherSplits
            }

            /**
             * Assign the unattributed time to the current crystal, and reset the unattributed time.
             * Pass the nucleusCrystalType to assign the unattributed time to a specific crystal.
             */
            fun saveSplit(nucleusCrystalType: NucleusCrystalType? = null) {
                val key = nucleusCrystalType ?: inProgressCrystal ?: return
                splits[key] = unattributedTime
                unattributedTime = 0.milliseconds
            }
        }

        enum class SpeedrunTransitionType {
            HOTKEY_CLICK,
            CRYSTAL_COLLECTED,
            NUCLEUS_LEFT,
            RUN_COMPLETED,
            RUN_ABORTED,
            CRYSTAL_PLACED,
        }
    }

    private val config get() = SkyHanniMod.feature.mining.crystalNucleusSpeedrun
    private val currentRun: Data.CrystalNucleusSpeedrun? get() {
        var run: Data.CrystalNucleusSpeedrun? = null
        tracker.modify { run = it.currentRun }
        return run
    }
    private val bestRun: Data.CrystalNucleusSpeedrun? get() {
        var run: Data.CrystalNucleusSpeedrun? = null
        tracker.modify { run = it.bestRun }
        return run
    }
    private val crystalSplitOrder: List<NucleusCrystalType> get() =
        config.manualSplitOrder.takeIf { it.size == CRYSTALS_TO_PLACE } ?: config.defaultSplitOrder
    private const val CRYSTALS_TO_PLACE = 5

    private var lastCollectedCrystal: MutableMap<NucleusCrystalType, SimpleTimeMark> = mutableMapOf()
    private var lastHotkeyClick: SimpleTimeMark = farPast()
    private var waitingAfterSplit: Boolean = false
    private var tpSinceSplit: Boolean = false
    private var lastLorenzVec: LorenzVec? = null
    private var movedSinceSplit: Boolean = false

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shnucspeedrun") {
            description = "Toggles the Crystal Nucleus Speedrun Tracker."
            category = CommandCategory.USERS_ACTIVE
            aliases = listOf("shnsr")
            callback {
                config.enabled = !config.enabled
                ChatUtils.chat("Crystal Nucleus Speedrun Tracker ${if (config.enabled) "§aenabled" else "§cdisabled"}")
            }
        }
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!config.enabled || currentRun?.state != SpeedrunState.IN_PROGRESS) return
        forceUpdate()
    }

    @HandleEvent
    fun onWarp(event: SkyHanniWarpEvent) {
        if (!config.enabled || !waitingAfterSplit) return
        tpSinceSplit = true
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onPlayerMove(event: EntityMoveEvent<EntityPlayerSP>) {
        if (!event.isLocalPlayer || !waitingAfterSplit || !tpSinceSplit) return
        lastLorenzVec = event.entity.getLorenzVec().takeIf { it != lastLorenzVec } ?: return
        movedSinceSplit = true
    }

    @HandleEvent
    fun onKeyPress(event: KeyPressEvent) {
        if (!config.enabled || event.keyCode != config.hotkey) return
        if (lastHotkeyClick.passedSince() < 1.seconds) return
        forceUpdate(SpeedrunTransitionType.HOTKEY_CLICK)
        lastHotkeyClick = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onCrystalPlaced(event: CrystalNucleusCrystalPlacedEvent) {
        if (!config.enabled) return
        forceUpdate(SpeedrunTransitionType.CRYSTAL_PLACED)
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onCrystalFound(event: CrystalNucleusCrystalFoundEvent) {
        if (!config.enabled) return
        lastCollectedCrystal[event.crystalType] = SimpleTimeMark.now()
        forceUpdate(SpeedrunTransitionType.CRYSTAL_COLLECTED)
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onCrystalNucleusLoot(event: CrystalNucleusLootEvent) {
        if (!config.enabled) return
        forceUpdate(SpeedrunTransitionType.RUN_COMPLETED)
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (!config.enabled) return
        if (event.newIsland != IslandType.CRYSTAL_HOLLOWS) forceUpdate(SpeedrunTransitionType.RUN_ABORTED)
    }

    private fun drawDisplay(data: Data): List<Searchable> {
        return emptyList() // Todo
    }

    private fun forceUpdate(transitionType: SpeedrunTransitionType? = null) {
        val currentState = currentRun?.state ?: return
        val nextState = transitionType?.let { nextStateMapLogic[currentState]?.invoke(it) } ?: currentState

        if (currentState == nextState) stayStateLogicMap[currentState]?.invoke()
        else transitionStateMapLogic[currentState]?.invoke()

        tracker.modify { it.currentRun.state = nextState }
    }

    private fun saveAndDisposeRun() {
        val currentRun = currentRun ?: return
        if (currentRun.splits.size < CRYSTALS_TO_PLACE) return

        val splits = currentRun.splits.toMap()
        val storedBestRun = bestRun ?: return
        val newBestRun = currentRun.takeIf { it.isBetterThan(storedBestRun) } ?: storedBestRun

        tracker.modify {
            it.cacheSplits(splits.toMutableMap())
            it.runsCompleted++
            it.currentRun = Data.CrystalNucleusSpeedrun()
            it.bestRun = newBestRun
        }
    }

    private val tStart: RunStartType get() = config.runStartType
    private val tSplit: RunSplitType get() = config.runSplitType
    private val tStop: RunStopType get() = config.runStopType

    // <editor-fold desc="Next state logic">
    /**
     * Returns the next state based on the current state and the transition type.
     * The key param is the 'current' state, and the value param is the trigger for the state change.
     * This will not always lead to a state change, as the value param may return null or the same state.
     */
    private val nextStateMapLogic: Map<SpeedrunState, (SpeedrunTransitionType) -> SpeedrunState?> =
        mapOf(
            // Not started -> Next state
            SpeedrunState.NOT_STARTED to { transitionType ->
                when (transitionType) {
                    SpeedrunTransitionType.HOTKEY_CLICK ->
                        SpeedrunState.IN_PROGRESS.takeIf { tStart == RunStartType.HOTKEY }

                    SpeedrunTransitionType.NUCLEUS_LEFT ->
                        SpeedrunState.IN_PROGRESS.takeIf { tStart == RunStartType.LEAVE_CRYSTAL_NUCLEUS }

                    else -> null
                }
            },

            // In progress -> Next state
            SpeedrunState.IN_PROGRESS to { transitionType ->
                when (transitionType) {
                    // Split if applicable
                    SpeedrunTransitionType.HOTKEY_CLICK -> SpeedrunState.SPLITTING.takeIf { tSplit == RunSplitType.HOTKEY }
                    SpeedrunTransitionType.CRYSTAL_COLLECTED -> SpeedrunState.SPLITTING.takeIf { tSplit == RunSplitType.AUTO }

                    // Stop the run if applicable
                    SpeedrunTransitionType.CRYSTAL_PLACED -> {
                        val currentlyCollectedSize = currentRun?.currentlyCollectedCrystals?.size ?: return@to null
                        SpeedrunState.SPLITTING.takeIf {
                            tStop == RunStopType.LAST_CRYSTAL_COLLECTED && currentlyCollectedSize == CRYSTALS_TO_PLACE
                        }
                    }

                    // If the run is aborted or completed, we want to reset the state
                    SpeedrunTransitionType.RUN_ABORTED, SpeedrunTransitionType.RUN_COMPLETED -> SpeedrunState.NOT_STARTED

                    else -> null
                }
            },

            // Splitting -> Next state
            SpeedrunState.SPLITTING to { _: SpeedrunTransitionType ->
                val currentRun = currentRun ?: return@to null
                if (currentRun.splits.size == CRYSTALS_TO_PLACE) SpeedrunState.NOT_STARTED
                else when (tSplit) {
                    RunSplitType.HOTKEY -> SpeedrunState.IN_PROGRESS
                    RunSplitType.AUTO -> SpeedrunState.IDLE_WAITING
                }
            },

            // Idle (waiting) -> Next state
            SpeedrunState.IDLE_WAITING to { _: SpeedrunTransitionType ->
                if (!tpSinceSplit || !movedSinceSplit) return@to null
                SpeedrunState.IN_PROGRESS
            }
        )
    // </editor-fold>

    // <editor-fold desc="Stay-state logic">
    /**
     * The logic that will take place if the state is NOT to be changed.
     * This will involve either 'refreshing' the state determining data, or doing nothing.
     */
    private val stayStateLogicMap: Map<SpeedrunState, () -> Unit> =
        mapOf(
            // No-op, all starts are handled in the next state logic
            SpeedrunState.NOT_STARTED to { },
            // This should never happen (but just in case)
            SpeedrunState.SPLITTING to { },
            SpeedrunState.IN_PROGRESS to {
                val currentRun: Data.CrystalNucleusSpeedrun = currentRun ?: return@to
                val currentCrystals = currentRun.currentlyCollectedCrystals
                if (!currentRun.isInitialized()) {
                    val firstCrystal = crystalSplitOrder.firstOrNull { it !in currentCrystals } ?: return@to
                    tracker.modify {
                        it.currentRun.init(firstCrystal)
                    }
                }

                // Update the unattributed time
                val startTime = currentRun.startedAt
                val previousSums = currentRun.splits.values.sumOf { it.inWholeMilliseconds }
                val currentCrystalDuration = (startTime.passedSince() - previousSums.milliseconds)
                tracker.modify {
                    it.currentRun.unattributedTime = currentCrystalDuration
                }
            },
            // Again, waiting for external trigger(s)
            SpeedrunState.IDLE_WAITING to { },
        )
    // </editor-fold>

    // <editor-fold desc="Transition logic">
    /**
     * Logic that will take place when transitioning from one state to another.
     * The key param is the 'next' state, the value params are the trigger for the state change and the old state.
     */
    private val transitionStateMapLogic: Map<SpeedrunState, () -> Unit> =
        mapOf(
            SpeedrunState.IDLE_WAITING to {
                // Reset the tp and move checks
                tpSinceSplit = false
                movedSinceSplit = false
            },
            SpeedrunState.SPLITTING to {
                // Determine next crystal
                val currentRun: Data.CrystalNucleusSpeedrun = currentRun?.takeIf { it.isInitialized() } ?: return@to
                val currentCrystals = currentRun.currentlyCollectedCrystals

                // Check to make sure last collected crystal matches with the current crystal
                val recentlyCollectedCrystal = lastCollectedCrystal.maxByOrNull { it.value }?.key
                val currentCrystalWorkingOn = currentRun.inProgressCrystal
                if (recentlyCollectedCrystal != null && recentlyCollectedCrystal != currentCrystalWorkingOn) {
                    tracker.modify {
                        it.currentRun.inProgressCrystal = recentlyCollectedCrystal
                    }
                }

                // Save the split
                tracker.modify {
                    it.currentRun.saveSplit()
                }

                // If the run is completed (i.e. all crystals have been collected), state transition logic
                // will take care of this, so we can just return here.
                val nextCrystal = crystalSplitOrder.firstOrNull { it !in currentCrystals } ?: return@to

                // Set the next crystal
                tracker.modify {
                    it.currentRun.inProgressCrystal = nextCrystal
                }
            },
            SpeedrunState.NOT_STARTED to {
                val currentRun = currentRun ?: return@to
                // If the run is completed (i.e. all crystals have been collected), we want to save the run
                if (currentRun.splits.size == CRYSTALS_TO_PLACE) saveAndDisposeRun()
            },
        )
    // </editor-fold>
}
