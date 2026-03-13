package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureFishEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi.babySlugName
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.Minecraft
import kotlin.reflect.KMutableProperty0
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FishingTimer {

    private val config get() = SkyHanniMod.feature.fishing.barnTimer
    private val barnLocation = LorenzVec(108, 89, -252)
    private val mobDespawnTime = mutableMapOf<Mob, SimpleTimeMark>()

    // TODO: Form into data class, and use Resettable
    private var lastSeaCreatureFished = SimpleTimeMark.farPast()
    private var display: Renderable? = null
    private var lastNameFished: String? = null

    private var babyMagmaSlugsToFind = 0
    private var lastMagmaSlugLocation: LorenzVec? = null
    private var lastMagmaSlugTime = SimpleTimeMark.farPast()
    private var recentBabyMagmaSlugs = TimeLimitedSet<Mob>(2.seconds)

    private var mobsToFind = 0

    private val recentMobs = TimeLimitedSet<Mob>(2.seconds)
    private val currentCap by RecalculatingValue(1.seconds) {
        when (SkyBlockUtils.currentIsland) {
            IslandType.CRYSTAL_HOLLOWS -> 20
            IslandType.CRIMSON_ISLE -> 5
            else -> config.fishingCapAmount
        }
    }

    private var rightLocation = false
    private var currentCount = 0
    private var startTime = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!config.enabled.get()) return

        if (babyMagmaSlugsToFind != 0 && lastMagmaSlugTime.passedSince() > 3.seconds) {
            babyMagmaSlugsToFind = 0
            lastMagmaSlugLocation = null
        }

        rightLocation = updateLocation()
        if (startTime.passedSince().inWholeSeconds - config.alertTime in 0..3) {
            playSound()
        }
        if (config.wormLimitAlert && IslandType.CRYSTAL_HOLLOWS.isCurrent()) {
            if (currentCount >= 20) {
                playSound()
                TitleManager.sendTitle("§cWORM CAP FULL!!!", duration = 2.seconds)
            }
        } else if (config.fishingCapAlert && currentCount >= currentCap) {
            playSound()
        }
    }

    private fun playSound() = SoundUtils.repeatSound(250, 4, SoundUtils.plingSound)

    @HandleEvent(onlyOnSkyblock = true)
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!config.enabled.get()) return
        val mob = event.mob
        if (mob.name == babySlugName) {
            recentBabyMagmaSlugs += mob
            DelayedRun.runNextTick {
                handleBabySlugs()
            }
            return
        }
        if (mob.name !in SeaCreatureManager.allFishingMobs) return
        recentMobs += mob
        handle()
    }

    @HandleEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        val mob = event.mob
        recentBabyMagmaSlugs -= event.mob
        if (mob in mobDespawnTime) {
            mobDespawnTime.remove(mob)
            if (mob.name == "Magma Slug") {
                lastMagmaSlugLocation = mob.baseEntity.getLorenzVec()
                babyMagmaSlugsToFind += 3
                lastMagmaSlugTime = SimpleTimeMark.now()
                handleBabySlugs()
            }
        }
        recentMobs -= mob
        updateInfo()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        if (!config.enabled.get()) return
        if (!rightLocation) return
        lastSeaCreatureFished = SimpleTimeMark.now()
        lastNameFished = event.seaCreature.name
        mobsToFind = if (event.doubleHook) 2 else 1
        handle()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onKeyDown(event: KeyDownEvent) {
        if (!config.enabled.get()) return
        if (Minecraft.getInstance().screen != null) return
        if (event.keyCode != config.manualResetTimer) return

        mobDespawnTime.replaceAll { _, _ ->
            SimpleTimeMark.now()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        if (!fishingDependentEnabled()) return
        display = createDisplay()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!fishingDependentEnabled()) return
        val display = display ?: return
        config.pos.renderRenderable(display, posLabel = "BarnTimer")
    }

    @HandleEvent
    fun onWorldChange() {
        mobDespawnTime.clear()
        recentMobs.clear()
        babyMagmaSlugsToFind = 0
        display = null
        lastMagmaSlugLocation = null
        lastMagmaSlugTime = SimpleTimeMark.farPast()
        recentBabyMagmaSlugs.clear()
        mobsToFind = 0
        currentCount = 0
        startTime = SimpleTimeMark.farPast()
    }

    private fun fishingDependentEnabled() = config.enabled.get() && rightLocation && currentCount > 0 && FishingApi.isFishing()

    private fun handleFishedMobs(
        lastEventTime: SimpleTimeMark,
        mobList: TimeLimitedSet<Mob>,
        toFind: KMutableProperty0<Int>,
        filter: (Mob) -> Boolean = { true },
        sortKey: (Mob) -> Double,
        onAllFound: () -> Unit,
    ) {
        if (lastEventTime.passedSince() > 2.seconds) return
        if (toFind.get() == 0) return
        val filteredMobs = mobList.filter { filter(it) && it !in mobDespawnTime }
        val mobs = filteredMobs.sortedBy { sortKey(it) }.take(toFind.get())
        if (mobs.isEmpty()) return
        toFind.set(toFind.get() - mobs.size)
        mobs.forEach { mobDespawnTime[it] = SimpleTimeMark.now() }
        if (toFind.get() == 0) {
            mobList.clear()
            onAllFound()
        }
        updateInfo()
    }

    private fun handle() = lastNameFished?.let { lastName ->
        handleFishedMobs(
            lastEventTime = lastSeaCreatureFished,
            mobList = recentMobs,
            toFind = ::mobsToFind,
            filter = { it.name == lastName },
            sortKey = { it.baseEntity.distanceToPlayer() },
            onAllFound = { lastNameFished = null },
        )
    }

    private fun handleBabySlugs() = lastMagmaSlugLocation?.let { location ->
        handleFishedMobs(
            lastEventTime = lastMagmaSlugTime,
            mobList = recentBabyMagmaSlugs,
            toFind = ::babyMagmaSlugsToFind,
            sortKey = { it.baseEntity.distanceTo(location) },
            onAllFound = { lastMagmaSlugLocation = null },
        )
    }

    private fun updateInfo() {
        currentCount = mobDespawnTime.size
        startTime = mobDespawnTime.values.maxByOrNull { it.passedSince() } ?: SimpleTimeMark.farPast()
        display = createDisplay()
    }

    private fun updateLocation(): Boolean {
        if (config.showAnywhere) return true

        return when (SkyBlockUtils.currentIsland) {
            IslandType.CRYSTAL_HOLLOWS -> config.crystalHollows.get()
            IslandType.CRIMSON_ISLE -> config.crimsonIsle.get()
            IslandType.WINTER -> config.winterIsland.get()
            IslandType.HUB -> barnLocation.distanceToPlayer() < 50
            IslandType.PRIVATE_ISLAND -> config.forStranded.get() && SkyBlockUtils.isStrandedProfile
            else -> false
        }
    }

    private fun createDisplay(): Renderable {
        val passedSince = startTime.passedSince()
        val timeColor = if (passedSince > config.alertTime.seconds) "§c" else "§e"
        val timeFormat = passedSince.format(TimeUnit.MINUTE)
        val countColor = if (config.fishingCapAlert && currentCount >= currentCap) "§c" else "§e"
        val name = StringUtils.pluralize(currentCount, "sea creature")
        return Renderable.text("$timeColor$timeFormat §8($countColor$currentCount §b$name§8)")
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Barn Fishing Timer")
        event.addIrrelevant {
            add("lastSeaCreatureFished: $lastSeaCreatureFished")
            add("lastNameFished: $lastNameFished")
            add("babyMagmaSlugsToFind: $babyMagmaSlugsToFind")
            add("lastMagmaSlugLocation: $lastMagmaSlugLocation")
            add("lastMagmaSlugTime: $lastMagmaSlugTime")
            add("recentBabyMagmaSlugs: $recentBabyMagmaSlugs")
            add("mobsToFind: $mobsToFind")
            add("recentMobs: $recentMobs")
            add("currentCap: $currentCap")
            add("mobDespawnTime: $mobDespawnTime")
            add("startTime: $startTime")
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "fishing.barnTimer", "fishing.barnTimer.enabled")
        event.move(3, "fishing.barnTimerAlertTime", "fishing.barnTimer.alertTime")
        event.move(3, "fishing.barnTimerCrystalHollows", "fishing.barnTimer.crystalHollows")
        event.move(3, "fishing.barnTimerForStranded", "fishing.barnTimer.forStranded")
        event.move(3, "fishing.wormLimitAlert", "fishing.barnTimer.wormLimitAlert")
        event.move(3, "fishing.manualResetTimer", "fishing.barnTimer.manualResetTimer")
        event.move(3, "fishing.barnTimerPos", "fishing.barnTimer.pos")
    }
}
