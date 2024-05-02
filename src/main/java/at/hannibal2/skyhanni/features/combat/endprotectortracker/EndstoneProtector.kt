package at.hannibal2.skyhanni.features.combat.endprotectortracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.replaceAll
import at.hannibal2.skyhanni.utils.TimeUtils.inWholeTicks
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

object EndstoneProtector {

    val config get() = SkyHanniMod.feature.combat.endstoneCounter
    private var countdownStart: SimpleTimeMark = SimpleTimeMark.farPast()
    private var shouldCountdown = false
    val storage get() = ProfileStorageData.profileSpecific?.endstoneProtectorTracker
    private var inNest = false
    private var stage: Stages = Stages.Resting
    private var golemDead = true
    private var golemFightTime: SimpleTimeMark = SimpleTimeMark.farPast()
    private var golemPos: LorenzVec = LorenzVec()
    private var seenSpawnMessage = false
    private var golemDiedRecently = false
    private var rose = false
    private var endstone = false
    private var fragment = false
    private var fourthDrop = false

    private val patternGroup = RepoPattern.group("combat.endstoneprotectortracker")
    private val stagePattern by patternGroup.pattern(
        "stage",
        ".*Protector: §r§[0-9a-fA-F](?<stage>[A-Za-z]+)\$"
    )
    private val alivePattern by patternGroup.pattern(
        "alive",
        "§c§lBEWARE - An Endstone Protector has risen!"
    )
    private val deadPattern by patternGroup.pattern(
        "dead",
        "§f\\s+§r§6§lENDSTONE PROTECTOR DOWN!"
    )
    private val stageChangePattern by patternGroup.pattern(
        "stagechange",
        "§c§lYou feel a tremor from beneath the earth!"
    )
    private val stage5Pattern by patternGroup.pattern(
        "stage5",
        "§c§lThe ground begins to shake as an Endstone Protector rises from below!"
    )
    private val damagePattern by patternGroup.pattern(
        "damage",
        "§f\\s+§r§eYour Damage: §r§a(?<damage>.*\\d) .*"
    )

    private val tracker = SkyHanniTracker("Endstone Protector Tracker", { Data() }, { it.endstoneProtectorTracker }) {
        formatDisplay(
            drawDisplay(it)
        )
    }

    private enum class Stages(val y: Int) {
        Resting(0),
        Dormant(5),
        Agitated(6),
        Disturbed(7),
        Awakening(8),
        Summoned(9),
    }

    class Data : TrackerData() {

        override fun reset() {
            totalKilled = 0
            sinceLastTierBoost = 0
            sinceLastEpicGolem = 0
            sinceLastLegGolem = 0
            tierboosts = 0
            epicGolems = 0
            legGolems = 0
            crystalFragments = 0
            enchEndstones = 0
            endstoneRoses = 0
        }

        @Expose
        var totalKilled = 0

        @Expose
        var tierboosts = 0

        @Expose
        var sinceLastTierBoost = 0

        @Expose
        var epicGolems = 0

        @Expose
        var sinceLastEpicGolem = 0

        @Expose
        var legGolems = 0

        @Expose
        var sinceLastLegGolem = 0

        @Expose
        var crystalFragments = 0

        @Expose
        var enchEndstones = 0

        @Expose
        var endstoneRoses = 0

    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (config.enabledCountdown && shouldCountdown)
            config.guiposition.renderString(countdownStart.plus(20.seconds).timeUntil()
                .toString(DurationUnit.SECONDS, 2), 0, 10, posLabel = "Endstone Protector Spawn Countdown")

        if (config.enabledGUI) config.guiposition.renderString("§eStage§6: §b${stage.ordinal}", posLabel = "Endstone Protector Stage Display")
        if (config.onlyOnNest && !inNest) return
        tracker.renderDisplay(config.position)
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.textFormat.afterChange {
            tracker.update()
        }
        tracker.update()
    }

    private fun formatDisplay(map: List<List<Any>>): List<List<Any>> {
        if (!ProfileStorageData.loaded) return emptyList()

        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat.get()) {
            // TODO, change functionality to use enum rather than ordinals
            newList.add(map[index.ordinal])
        }
        return newList
    }

    fun resetTracker() {
        tracker.resetCommand()
    }

    private fun drawDisplay(data: Data) = buildList<List<Any>> {
        addAsSingletonList("§6§lEndstone Protector Tracker")
        addAsSingletonList("§eTotal Killed§f: ${data.totalKilled.addSeparators()}")
        addAsSingletonList("§6Tier Boost Core§f: §b${data.tierboosts} ")
        addAsSingletonList("§6Kills Since Tier Boost§f: §b${data.sinceLastTierBoost} ")
        addAsSingletonList("§6Legendary Golem Pets§f: §b${data.legGolems} ")
        addAsSingletonList("§6Kills Since Legendary Golem§f: §b${data.sinceLastLegGolem} ")
        addAsSingletonList("§5Epic Golem Pets§f: §b${data.epicGolems} ")
        addAsSingletonList("§5Kills Since Epic Golem§f: §b${data.sinceLastEpicGolem} ")
        addAsSingletonList("§5Crystal Fragment§f: §b${data.crystalFragments} ")
        addAsSingletonList("§aEnchanted End Stone§f: §b${data.enchEndstones} ")
        addAsSingletonList("§9Endstone Rose§f: §b${data.endstoneRoses} ")
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        inNest = LorenzUtils.skyBlockArea == "Dragon's Nest"
        if (inNest && config.showLocation && golemDead && stage != Stages.Resting && golemPos.isZero()) scanLocation()
        if (golemDiedRecently) {
            if (event.repeatSeconds(5)) golemDiedRecently = false
            if (!rose || !endstone || !fragment || !fourthDrop) checkForLoot()
        }
    }

    private fun checkForLoot() {
        EntityUtils.getEntities<EntityArmorStand>().forEach { entityArmorStand ->
            var name = entityArmorStand.name
            var amount = 1

            if (" §8x" in name) {
                val parts = name.split(" §8x")
                amount = parts[1].toInt()
                name = parts[0]
            }

            when (name) {
                "§9Endstone Rose" -> {
                    if (rose) return
                    tracker.modify { storage -> storage.endstoneRoses += amount }
                    rose = true
                }
                "§aEnchanted End Stone" -> {
                    if (endstone) return
                    tracker.modify { storage -> storage.enchEndstones += amount }
                    endstone = true
                }
                "§5Crystal Fragment" -> {
                    if (fragment) return
                    tracker.modify { storage -> storage.crystalFragments += amount }
                    fragment = true
                }
                "§7[Lvl 1] §5Golem" -> {
                    if (fourthDrop) return
                    tracker.modify { storage ->
                        storage.epicGolems += amount
                        storage.sinceLastEpicGolem = 0
                        dropAlert("§5GOLEM")
                    }
                    fourthDrop = true
                }
                "§7[Lvl 1] §6Golem" -> {
                    if (fourthDrop) return
                    tracker.modify { storage ->
                        storage.legGolems += amount
                        storage.sinceLastLegGolem = 0
                        dropAlert("§6GOLEM")
                    }
                    fourthDrop = true
                }
                "§6Tier Boost Core" -> {
                    if (fourthDrop) return
                    tracker.modify { storage ->
                        storage.tierboosts += amount
                        storage.sinceLastTierBoost = 0
                        dropAlert("§6TIER BOOST")
                    }
                    fourthDrop = true
                }
                else -> {}
            }
        }
    }

    private fun scanLocation() {
        val y = stage.y
        val positions = listOf(
            LorenzVec(-649, y, -219),
            LorenzVec(-644, y, -269),
            LorenzVec(-689, y, -273),
            LorenzVec(-727, y, -284),
            LorenzVec(-639, y, -328),
            LorenzVec(-678, y, -332),
        )

        for (pos in positions) {
            if (Minecraft.getMinecraft().theWorld.getBlockState(pos.toBlockPos()).block == Blocks.skull) {
                golemPos = pos
                return
            }
        }
    }

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (config.showLocation && inNest && !golemPos.isZero()) {
            if (config.showOnlyIfStage4or5 && (stage == Stages.Summoned || stage == Stages.Awakening)) {
                event.drawDynamicText(golemPos, "GOLEM", 1.0)
                return
            }
            if (stage != Stages.Resting) {
                event.drawDynamicText(golemPos, "GOLEM", 1.0)
            }
        }
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!isEnabled()) return
        for (line in event.tabList) {
            stagePattern.matchMatcher(line) {
                val tabStage = group("stage")
                stage = when(tabStage) {
                    "Resting" -> Stages.Resting
                    "Dormant" -> Stages.Dormant
                    "Agitated" -> Stages.Agitated
                    "Disturbed" -> Stages.Disturbed
                    "Awakening" -> Stages.Awakening
                    "Summoned" -> Stages.Summoned
                    else -> {Stages.Resting}
                }
            }
        }
    }

    private var dmg = 0
    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        alivePattern.matchMatcher(event.message) {
            golemDead = false
            golemFightTime = SimpleTimeMark.now()
            seenSpawnMessage = true
            shouldCountdown = false
            resetDrops()
        }
        deadPattern.matchMatcher(event.message) {
            sendStats(SimpleTimeMark.now().minus(golemFightTime).inWholeTicks)
            stage = Stages.Resting
            golemPos = LorenzVec()
        }
        stage5Pattern.matchMatcher(event.message) {
            stage = Stages.Summoned
            shouldCountdown = true
            countdownStart = SimpleTimeMark.now()
            if (config.stage5Alert) stage5alert()
        }
        stageChangePattern.matchMatcher(event.message) {
            if (config.stage4Alert && stage == Stages.Awakening) stage4alert()
        }
        damagePattern.matchMatcher(event.message) {
            dmg = group("damage").replaceAll(",", "").toInt()

            if (dmg > 0 && !golemDead) {
                tracker.modify { storage ->
                    storage.totalKilled += 1
                    storage.sinceLastLegGolem += 1
                    storage.sinceLastEpicGolem += 1
                    storage.sinceLastTierBoost += 1
                }
                golemDiedRecently = true
                golemDead = true
            }
        }
    }

    private fun sendStats(time: Int) {
        DelayedRun.runNextTick {
            if (seenSpawnMessage) {
                if (config.showFightTime) ChatUtils.chat("Golem Fight Took: §f${time.div(20)} Seconds")
                if (config.showFightDPS) ChatUtils.chat("DPS: §f${dmg.div(time.div(20)).addSeparators()}")
                seenSpawnMessage = false
            }
            else ChatUtils.chat("Unknown Golem Spawn Time.")
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        inNest = false
        stage = Stages.Resting
        golemPos = LorenzVec()
        golemDead = true
        golemFightTime = SimpleTimeMark.farPast()
        seenSpawnMessage = false
        golemDiedRecently = false
        shouldCountdown = false
        resetDrops()
    }

    private fun stage5alert() {
        LorenzUtils.sendTitle("§4STAGE 5", 2.seconds)
        SoundUtils.createSound("random.anvil_land", 1f).playSound()
    }

    private fun stage4alert() {
        LorenzUtils.sendTitle("§cSTAGE 4", 2.seconds)
        SoundUtils.createSound("mob.irongolem.hit", 1f).playSound()
    }

    private fun dropAlert(item: String) {
        if (!config.dropAlert) return
        LorenzUtils.sendTitle(item, 2.seconds)
        SoundUtils.playBeepSound()
    }

    private fun resetDrops() {
        rose = false
        endstone = false
        fragment = false
        fourthDrop = false
    }

    fun isEnabled() = IslandType.THE_END.isInIsland() && (config.enabled || config.enabledGUI)
}