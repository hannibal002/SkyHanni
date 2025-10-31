package at.hannibal2.hanni.features.rift

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.features.rift.RiftConfig
import at.hannibal2.hanni.data.IslandGraphs
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.mob.Mob
import at.hannibal2.hanni.events.MobEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.SkyBlockItemModifierUtils.isRiftExportable
import at.hannibal2.hanni.utils.SkyBlockItemModifierUtils.wasRiftTransferred
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.getLorenzVec
import net.minecraft.item.ItemStack

@HanniModule
object RiftApi {

    fun inRift() = IslandType.THE_RIFT.isCurrent()

    val config: RiftConfig get() = HanniMod.feature.rift

    // internal name -> motes
    var motesPrice = emptyMap<NeuInternalName, Double>()

    val farmingTool = "FARMING_WAND".toInternalName()

    private val blowgun = "BERBERIS_BLOWGUN".toInternalName()

    val ItemStack?.isBlowgun: Boolean
        get() = this?.getInternalName() == blowgun

    fun ItemStack.motesNpcPrice(): Double? {
        if (isRiftExportable() && wasRiftTransferred()) return null
        return getInternalName().motesNpcPrice()?.times(stackSize)
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

    @HandleEvent
    fun onAreaChange(event: GraphAreaChangeEvent) {
        inMirrorVerse = event.area == "Mirrorverse"
        inColosseum = event.area == "Colosseum"
    }

    private val temporalPillars = mutableListOf<Mob>()

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (event.mob.name == "Temporal Pillar") {
            temporalPillars.add(event.mob)
        }
    }

    @HandleEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (event.mob.name == "Temporal Pillar") {
            temporalPillars.remove(event.mob)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!config.temporalPillarDodge) {
            if (IslandGraphs.disabledNodesReason == "Temporal Pillar") {
                IslandGraphs.enableAllNodes()
            }
            return
        }

        IslandGraphs.disabledNodesReason?.let {
            IslandGraphs.enableAllNodes()
            if (temporalPillars.isEmpty()) {
                IslandGraphs.update(force = true)
            }
        }

        if (temporalPillars.isNotEmpty()) {
            for (mob in temporalPillars) {
                val location = mob.baseEntity.getLorenzVec()
                IslandGraphs.disableNodes("Temporal Pillar", location, 7.0)
            }
            IslandGraphs.update(force = true)
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
