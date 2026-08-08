package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.rift.RiftConfig
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRiftExportable
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.wasRiftTransferred
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpace
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object RiftApi {

    /**
     * REGEX-TEST: Motes: 137,242
     */
    private val motesPattern by RepoPattern.pattern(
        "rift-api.motes",
        "Motes: (?<motes>[\\d,]+).*",
    )

    fun inRift() = IslandType.THE_RIFT.isInIsland()

    val config: RiftConfig get() = SkyHanniMod.feature.rift

    // internal name -> motes
    var motesPrice = emptyMap<NeuInternalName, Double>()

    val farmingTool = "FARMING_WAND".toInternalName()

    private val blowgun = "BERBERIS_BLOWGUN".toInternalName()

    val SafeItemStack?.isBlowgun: Boolean
        get() = this?.getInternalName() == blowgun

    fun SafeItemStack.motesNpcPrice(): Double? {
        if (isRiftExportable() && wasRiftTransferred()) return null
        return getInternalName().motesNpcPrice()?.times(count)
    }

    fun NeuInternalName.motesNpcPrice(): Double? {
        val baseMotes = motesPrice[this] ?: return null
        val burgerStacks = config.motes.burgerStacks
        return baseMotes + (burgerStacks * 5) * baseMotes / 100
    }

    var inMirrorVerse = false
    private var inColosseum = false
    var inRiftRace = false
    var trackingButtons = false
    var allButtonsHit = false

    // TODO: Cache this value and only update it when the scoreboard changes
    val motes: Int? get() {
        val scoreboardLines = ScoreboardData.sidebarLinesFormatted.map { it.removeColor().trimWhiteSpace() }
        return motesPattern.firstMatcher(scoreboardLines) {
            groupOrNull("motes")?.formatIntOrNull()
        }
    }

    @HandleEvent
    private fun onAreaChange(event: GraphAreaChangeEvent) {
        inMirrorVerse = event.area == "Mirrorverse"
        inColosseum = event.area == "Colosseum"
    }

    private val temporalPillars = mutableListOf<Mob>()

    @HandleEvent
    private fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (event.mob.name == "Temporal Pillar") {
            temporalPillars.add(event.mob)
        }
    }

    @HandleEvent
    private fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (event.mob.name == "Temporal Pillar") {
            temporalPillars.remove(event.mob)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    private fun onSecondPassed() {
        if (!config.temporalPillarDodge) {
            if (IslandGraphs.disabledNodesReason == "Temporal Pillar") {
                IslandGraphs.enableAllNodes()
            }
            return
        }

        IslandGraphs.disabledNodesReason?.let {
            IslandGraphs.enableAllNodes()
            if (temporalPillars.isEmpty()) {
                IslandGraphs.refreshNavigation(force = true)
            }
        }

        if (temporalPillars.isNotEmpty()) {
            for (mob in temporalPillars) {
                val location = mob.baseEntity.getLorenzVec()
                IslandGraphs.disableNodes("Temporal Pillar", location, 7.0)
            }
            IslandGraphs.refreshNavigation(force = true)
        }

    }

    // TODO use graph area for all those
    fun inLivingCave() = SkyBlockUtils.scoreboardArea == "Living Cave"
    fun inLivingStillness() = SkyBlockUtils.scoreboardArea == "Living Stillness"
    fun inStillgoreChateau() = SkyBlockUtils.scoreboardArea.let { it == "Stillgore Château" || it == "Oubliette" }
    fun inColosseum() = SkyBlockUtils.scoreboardArea == "Colosseum" || inColosseum
    fun inDreadfarm() = SkyBlockUtils.scoreboardArea == "Dreadfarm"
    fun inWestVillage() = SkyBlockUtils.scoreboardArea.let { it == "West Village" || it == "Infested House" }
    fun inMountainTop() = when (SkyBlockUtils.scoreboardArea) {
        "Continuum", "The Mountaintop", "Trial Grounds", "Time-Torn Isles",
        "Wizardman Bureau", "Wizard Brawl", "Walk of Fame", "Time Chamber",
        -> true

        else -> false
    }
}
