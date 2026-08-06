package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.api.HotmApi.PowderType
import at.hannibal2.skyhanni.api.SkillApi
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MaxwellApi.ThaumaturgyPowerTuning
import at.hannibal2.skyhanni.data.garden.CropCollectionApi
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.FarmingWeight
import at.hannibal2.skyhanni.data.jsonobjects.local.HotxTree
import at.hannibal2.skyhanni.data.model.ComposterUpgrade
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.features.combat.end.DragonProfitTracker
import at.hannibal2.skyhanni.features.combat.end.endernodetracker.EnderNodeTracker
import at.hannibal2.skyhanni.features.combat.ghosttracker.GhostTracker
import at.hannibal2.skyhanni.features.commands.OpenLastStorage
import at.hannibal2.skyhanni.features.dungeon.CroesusChestTracker.OpenedState
import at.hannibal2.skyhanni.features.dungeon.CroesusChestTracker.generateMaxChestAsList
import at.hannibal2.skyhanni.features.dungeon.DungeonFloor
import at.hannibal2.skyhanni.features.event.carnival.CarnivalGoal
import at.hannibal2.skyhanni.features.event.diana.DianaProfitTracker
import at.hannibal2.skyhanni.features.event.diana.MythologicalCreatureTracker
import at.hannibal2.skyhanni.features.event.hoppity.HoppityCollectionStats.LocationRabbit
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggType
import at.hannibal2.skyhanni.features.event.jerry.frozentreasure.FrozenTreasureTracker
import at.hannibal2.skyhanni.features.event.yearofthepig.ShinyOrbTracker
import at.hannibal2.skyhanni.features.fame.UpgradeReminder.CommunityShopUpgrade
import at.hannibal2.skyhanni.features.fishing.tracker.FishingProfitTracker
import at.hannibal2.skyhanni.features.fishing.tracker.SeaCreatureTracker
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.features.foraging.ForagingTrackerLegacy
import at.hannibal2.skyhanni.features.garden.CropAccessory
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.farming.lane.FarmingLane
import at.hannibal2.skyhanni.features.garden.leaderboarddisplays.CropLeaderboardStorage
import at.hannibal2.skyhanni.features.garden.leaderboarddisplays.PestLeaderboardStorage
import at.hannibal2.skyhanni.features.garden.leaderboarddisplays.WeightLeaderboardStorage
import at.hannibal2.skyhanni.features.garden.pests.stereo.VinylType
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi.PlotData
import at.hannibal2.skyhanni.features.garden.tracker.CropFeverTracker
import at.hannibal2.skyhanni.features.garden.tracker.GardenBpsTracker
import at.hannibal2.skyhanni.features.garden.tracker.PestProfitTracker
import at.hannibal2.skyhanni.features.garden.tracker.RareCropTracker
import at.hannibal2.skyhanni.features.garden.visitor.VisitorReward
import at.hannibal2.skyhanni.features.gifting.GiftProfitTracker
import at.hannibal2.skyhanni.features.hunting.HuntingProfitTracker
import at.hannibal2.skyhanni.features.inventory.CurrentEquipmentApi
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.stray.CFStrayTracker
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentsProfitTracker
import at.hannibal2.skyhanni.features.inventory.loadout.LoadoutApi.LoadoutData
import at.hannibal2.skyhanni.features.inventory.wardrobe.AbstractWardrobeApi.WardrobeData
import at.hannibal2.skyhanni.features.mining.DarkMonolithFeatures
import at.hannibal2.skyhanni.features.mining.MineshaftPityDisplay.PityData
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusTracker
import at.hannibal2.skyhanni.features.mining.fossilexcavator.ExcavatorProfitTracker
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftDetection
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.corpse.CorpseTracker
import at.hannibal2.skyhanni.features.mining.powdertracker.PowderTracker
import at.hannibal2.skyhanni.features.minion.InfernoMinionProfitTracker
import at.hannibal2.skyhanni.features.misc.DraconicSacrificeTracker
import at.hannibal2.skyhanni.features.misc.EnchantedClockHelper
import at.hannibal2.skyhanni.features.misc.trevor.TrevorTracker.TrapperMobRarity
import at.hannibal2.skyhanni.features.nether.reputationhelper.FactionType
import at.hannibal2.skyhanni.features.rift.area.mountaintop.TimiteTracker
import at.hannibal2.skyhanni.features.rift.area.westvillage.VerminTracker
import at.hannibal2.skyhanni.features.rift.area.westvillage.kloon.KloonTerminal
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.features.slayer.SlayerProfitTracker
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.NONE
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.farFuture
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.farPast
import at.hannibal2.skyhanni.utils.SkyblockCurrency
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import com.google.gson.annotations.Expose
import net.minecraft.network.chat.Component
import java.time.LocalDate
import java.util.EnumMap
import java.util.UUID
import kotlin.time.Duration

// put everything under its respective feature, the order of the features is the same as in the folder structure
class ProfileSpecificStorage(
    @Expose var profileName: String = "",
) {
    // api
    @Expose
    var skills: SkillStorage = SkillStorage()

    class SkillStorage {
        @Expose
        val skillData: MutableMap<SkillType, SkillApi.SkillInfo> = enumMapOf()

        @Expose
        var giftTalismanSkillXpBonus: Double = 0.0
    }

    @Expose
    var totalSkyBlockXP: Int? = null

    @Expose
    var crimsonIsleFaction: FactionType? = null

    /** Written and read by [at.hannibal2.skyhanni.data.CurrencyApi]. */
    @Expose
    val currencies: MutableMap<SkyblockCurrency, Long> = enumMapOf()

    /** Written and read by [at.hannibal2.skyhanni.data.CurrencyApi]. */
    @Expose
    val essences: MutableMap<NeuInternalName, Long> = mutableMapOf()

    // features
    // - combat
    @Expose
    var ghostStorage: GhostStorage = GhostStorage()

    class GhostStorage {
        @Expose
        var ghostTracker: GhostTracker.Data = GhostTracker.Data()

        @Expose
        var bestiaryKills: Long = 0L

        @Expose
        var migratedTotalKills: Boolean = false
    }

    @Expose
    val instanceChestFavoriteItems: MutableList<NeuInternalName> = mutableListOf()

    // - commands
    @Expose
    var lastStorage: LastStorage = LastStorage()

    class LastStorage {
        @Expose
        var type: OpenLastStorage.StorageType = OpenLastStorage.StorageType.ENDER_CHEST

        @Expose
        var page: Int? = null
    }

    // - dungeon
    @Expose
    var dungeons: DungeonStorage = DungeonStorage()

    class DungeonStorage {
        @Expose
        val bosses: MutableMap<DungeonFloor, Int> = enumMapOf()

        @Expose
        val runs: MutableList<DungeonRunInfo> = generateMaxChestAsList()

        class DungeonRunInfo {
            constructor()

            constructor(floor: String?, runTime: SimpleTimeMark?) {
                this.floor = floor
                this.runTime = runTime
                this.openState = OpenedState.UNOPENED
            }

            @Expose
            var runTime: SimpleTimeMark? = null

            @Expose
            var floor: String? = null

            @Expose
            var openState: OpenedState? = null
        }
    }

    @Expose
    var enderNodeTracker: EnderNodeTracker.Data = EnderNodeTracker.Data()

    @Expose
    var dragonProfitTracker: DragonProfitTracker.BucketData = DragonProfitTracker.BucketData()

    // - event
    // -- carnival
    @Expose
    var carnival: CarnivalStorage = CarnivalStorage()

    class CarnivalStorage {
        @Expose
        var lastClaimedDay: LocalDate? = null

        @Expose
        var carnivalYear: Int = 0

        @Expose
        val goals: MutableMap<CarnivalGoal, Boolean> = enumMapOf()

        // - shop name -> (item name, tier)
        @Expose
        val carnivalShopProgress: MutableMap<String, Map<String, Int>> = mutableMapOf()
    }

    // -- diana
    @Expose
    var diana: DianaStorage = DianaStorage()

    class DianaStorage {
        @Expose
        var profitTracker: DianaProfitTracker.Data = DianaProfitTracker.Data()

        @Expose
        val profitTrackerPerElection: MutableMap<Int, DianaProfitTracker.Data> = mutableMapOf()

        @Expose
        var mythologicalMobTracker: MythologicalCreatureTracker.Data = MythologicalCreatureTracker.Data()

        @Expose
        val mythologicalMobTrackerPerElection: MutableMap<Int, MythologicalCreatureTracker.Data> = mutableMapOf()
    }

    // -- winter
    @Expose
    var frozenTreasureTracker: FrozenTreasureTracker.Data = FrozenTreasureTracker.Data()

    @Expose
    var giftProfitTracker: GiftProfitTracker.Data = GiftProfitTracker.Data()

    // -- year of the [___]
    @Expose
    var shinyOrbTracker: ShinyOrbTracker.ShinyOrbData = ShinyOrbTracker.ShinyOrbData()

    // -- hoppity
    @Expose
    var chocolateFactory: CFStorage = CFStorage()

    class CFStorage {
        @Expose
        var currentRabbits: Int = 0

        @Expose
        var maxRabbits: Int = -1

        @Expose
        var currentChocolate: Long = 0

        @Expose
        var maxChocolate: Long = 0

        @Expose
        var chocolateThisPrestige: Long = 0

        @Expose
        var chocolateAllTime: Long = 0

        @Expose
        var rawChocPerSecond: Int = 0

        @Expose
        var chocolateMultiplier: Double = 1.0

        @Expose
        var rawChocolateMultiplier: Double = 1.0

        @Expose
        var timeTowerLevel: Int = 0

        @Expose
        var currentTimeTowerEnds: SimpleTimeMark = farPast()

        @Expose
        var nextTimeTower: SimpleTimeMark = farPast()

        @Expose
        var currentTimeTowerUses: Int = -1

        @Expose
        var timeTowerCooldown: Int = 8

        @Expose
        var maxTimeTowerUses: Int = 0

        @Expose
        var bestUpgradeAvailableAt: SimpleTimeMark = farPast()

        @Expose
        var bestUpgradeCost: Long = 0

        @Expose
        var lastDataSave: SimpleTimeMark = farPast()

        @Expose
        var positionChange: PositionChange = PositionChange()

        class PositionChange {
            @Expose
            var lastTime: SimpleTimeMark? = null

            @Expose
            var lastPosition: Int = -1

            @Expose
            var lastLeaderboard: String? = null
        }

        @Expose
        var targetGoal: Long? = null

        @Expose
        var targetName: String? = null

        @Expose
        val rabbitCounts: MutableMap<String, Int> = mutableMapOf()

        @Expose
        val locationRabbitRequirements: MutableMap<String, LocationRabbit> = mutableMapOf()

        @Expose
        val collectedEggLocations: MutableMap<IslandType, MutableSet<LorenzVec>> = enumMapOf()

        @Expose
        val residentRabbits: MutableMap<IslandType, MutableMap<String, Boolean?>> = enumMapOf()

        class HotspotRabbitStorage(@Expose var skyblockYear: Int?) {
            @Expose
            val hotspotRabbits: MutableMap<IslandType, MutableMap<String, Boolean?>> = enumMapOf()
        }

        @Expose
        var hotspotRabbitStorage: HotspotRabbitStorage = HotspotRabbitStorage(null)

        @Expose
        var hoppityShopYearOpened: Int? = null

        @Expose
        var strayTracker: CFStrayTracker.Data = CFStrayTracker.Data()

        @Expose
        val mealLastFound: MutableMap<HoppityEggType, SimpleTimeMark> = enumMapOf()

        @Expose
        val mealNextSpawn: MutableMap<HoppityEggType, SimpleTimeMark> = enumMapOf()

        @Expose
        var hotChocolateMixinExpiry = farPast()

        data class HitmanStatsStorage(
            @Expose var availableHitmanEggs: Int = 0,
            @Expose var singleSlotCooldownMark: SimpleTimeMark? = null,
            @Expose var allSlotsCooldownMark: SimpleTimeMark? = null,
            @Expose var purchasedHitmanSlots: Int = 0,
        ) : Resettable

        @Expose
        var hitmanStats: HitmanStatsStorage = HitmanStatsStorage()
    }

    @Expose
    val hoppityEventStats: MutableMap<Int, HoppityEventStats> = mutableMapOf()

    @Expose
    var hoppityStatLiveDisplayToggledOff: Boolean = false

    data class HoppityEventStats(
        @Expose var mealsFound: MutableMap<HoppityEggType, Int> = enumMapOf(),
        @Expose var rabbitsFound: MutableMap<LorenzRarity, RabbitData> = enumMapOf(),
        @Expose var dupeChocolateGained: Long = 0,
        @Expose var strayChocolateGained: Long = 0,
        @Expose var rabbitTheFishFinds: Int = 0,

        @Expose var millisInCf: Duration = Duration.ZERO,
        @Expose var initialLeaderboardPosition: LeaderboardPosition = LeaderboardPosition(-1, -1.0),
        @Expose var finalLeaderboardPosition: LeaderboardPosition = LeaderboardPosition(-1, -1.0),
        @Expose var lastLbUpdate: SimpleTimeMark = farPast(),
        @Expose var summarized: Boolean = false,

        @Expose var typeCountSnapshot: RabbitData? = RabbitData(),
        @Expose var typeCountsSince: RabbitData? = RabbitData(),
    ) {
        @Transient
        val containingYears: MutableSet<Int> = mutableSetOf()

        constructor(year: Int) : this() {
            containingYears.add(year)
        }

        constructor(years: Set<Int>) : this() {
            containingYears.addAll(years)
        }

        operator fun plusAssign(it: HoppityEventStats) {
            it.mealsFound.forEach { (key, value) ->
                mealsFound.merge(key, value, Int::plus)
            }
            it.rabbitsFound.forEach { (key, rabbitData) ->
                rabbitsFound.merge(key, rabbitData) { existing, new ->
                    RabbitData(
                        uniques = existing.uniques + new.uniques,
                        dupes = existing.dupes + new.dupes,
                        strays = existing.strays + new.strays,
                    )
                }
            }
            dupeChocolateGained += it.dupeChocolateGained
            strayChocolateGained += it.strayChocolateGained
            rabbitTheFishFinds += it.rabbitTheFishFinds
            millisInCf += it.millisInCf
        }

        companion object {
            data class RabbitData(
                @Expose var uniques: Int = 0,
                @Expose var dupes: Int = 0,
                @Expose var strays: Int = 0,
            ) {
                fun getByIndex(index: Int): Int = when (index) {
                    0 -> uniques
                    1 -> dupes
                    2 -> strays
                    else -> throw IllegalArgumentException("Invalid index: $index")
                }

                companion object {
                    val EMPTY get() = RabbitData(0, 0, 0)
                }
            }

            data class LeaderboardPosition(@Expose var position: Int, @Expose var percentile: Double)
        }
    }

    // - fame
    @Expose
    var communityShopProfileUpgrade: CommunityShopUpgrade? = null

    // - fishing
    @Expose
    var fishing: FishingStorage = FishingStorage()

    class FishingStorage {
        @Expose
        var fishingProfitTracker: FishingProfitTracker.Data = FishingProfitTracker.Data()

        @Expose
        var seaCreatureTracker: SeaCreatureTracker.Data = SeaCreatureTracker.Data()
    }

    // - garden
    @Expose
    var garden: GardenStorage = GardenStorage()

    class GardenStorage {
        @Expose
        var experience: Long? = null

        @Expose
        var lastMilestoneFix: SimpleTimeMark = farPast()

        @Expose
        var lastGainedCrop: CropType? = null

        @Expose
        var lastGainedCropCollectionTime: SimpleTimeMark = farPast()

        @Expose
        val cropCollectionCounter: MutableMap<CropType, CropCollectionApi.CropCollection> = enumMapOf()

        @Expose
        val cropMilestoneCounter: MutableMap<CropType, Long> = EnumMap(CropType::class.java)

        @Expose
        val toolCounterData: MutableMap<String, Long> = HashMap()

        @Expose
        val cropUpgrades: MutableMap<CropType, Int> = enumMapOf()

        @Expose
        val cropsPerSecond: MutableMap<CropType, Int> = enumMapOf()

        @Expose
        val latestBlocksPerSecond: MutableMap<CropType, Double> = enumMapOf()

        @Expose
        val latestTrueFarmingFortune: MutableMap<CropType, Double> = enumMapOf()

        @Expose
        val personalBestFF: MutableMap<CropType, Double> = enumMapOf()

        @Expose
        var savedCropAccessory: CropAccessory? = CropAccessory.NONE

        @Expose
        var informedAboutLowMatter: SimpleTimeMark = farPast()

        @Expose
        var informedAboutLowFuel: SimpleTimeMark = farPast()

        @Expose
        var visitorInterval: Long = 15 * 60000L

        @Expose
        var nextSixthVisitorArrival: SimpleTimeMark = farPast()

        @Expose
        var rareCropTracker: RareCropTracker.Data = RareCropTracker.Data()

        @Expose
        val composterUpgrades: MutableMap<ComposterUpgrade, Int> = enumMapOf()

        @Expose
        val toolWithBountiful: MutableMap<CropType, Boolean> = enumMapOf()

        @Expose
        var composterCurrentOrganicMatterItem: NeuInternalName? = NONE

        @Expose
        var composterCurrentFuelItem: NeuInternalName? = NONE

        @Expose
        var uniqueVisitors: Int = 0

        @Expose
        val charmedVisitors: MutableSet<String> = mutableSetOf()

        @Expose
        val ignoredVisitors: MutableSet<String> = mutableSetOf()

        @Expose
        var visitorDrops: VisitorDrops = VisitorDrops()

        // Todo: Move to a SkyhanniTracker (preferably bucketed by rarity)
        class VisitorDrops : Resettable {
            @Expose
            var acceptedVisitors: Int = 0

            @Expose
            var deniedVisitors: Int = 0

            fun getTotalVisitors() = acceptedVisitors + deniedVisitors

            @Expose
            val acceptedRarities: MutableMap<LorenzRarity, Long> = enumMapOf()

            @Expose
            var copper: Int = 0

            @Expose
            var farmingExp: Long = 0

            @Expose
            var gardenExp: Int = 0

            @Expose
            var coinsSpent: Long = 0

            @Expose
            var bits: Long = 0

            @Expose
            var mithrilPowder: Long = 0

            @Expose
            var gemstonePowder: Long = 0

            @Expose
            val rewardsCount: MutableMap<VisitorReward, Int> = enumMapOf()
        }

        @Expose
        var plotIcon: PlotIcon = PlotIcon()

        class PlotIcon {
            @Expose
            val plotList: MutableMap<Int, NeuInternalName> = mutableMapOf()
        }

        @Expose
        val plotData: MutableMap<Int, PlotData> = mutableMapOf()

        @Expose
        var scoreboardPests: Int = 0

        @Expose
        val cropStartLocations: MutableMap<CropType, LorenzVec> = enumMapOf()

        @Expose
        val cropLastFarmedLocations: MutableMap<CropType, LorenzVec> = enumMapOf()

        @Expose
        val farmingLanes: MutableMap<CropType, FarmingLane> = enumMapOf()

        @Expose
        var fortune: Fortune = Fortune()

        class Fortune {
            @Expose
            var farmingLevel: Int = -1

            @Expose
            var bestiary: Double = -1.0

            @Expose
            var cacao: Int = -1

            @Expose
            var relicOfPower: Double = -1.0

            @Expose
            var plotsUnlocked: Int = -1

            @Expose
            var anitaUpgrade: Int = -1

            @Expose
            var farmingStrength: Int = -1

            @Expose
            var cakeExpiring: SimpleTimeMark? = null

            @Expose
            val carrolyn: MutableMap<CropType, Boolean> = enumMapOf()
        }

        @Expose
        var composterEmptyTime: SimpleTimeMark = farPast()

        @Expose
        var lastComposterEmptyWarningTime: SimpleTimeMark = farPast()

        @Expose
        var farmingWeight: FarmingWeightConfig = FarmingWeightConfig()

        class FarmingWeightConfig {
            @Expose
            val lastLeaderboardPosMap: MutableMap<EliteLeaderboardType, Int> = mutableMapOf()

            @Expose
            val leaderboardAmountMap: MutableMap<EliteLeaderboardType, Double> = mutableMapOf()

            @Expose
            var cropDisplayType: CropLeaderboardStorage = CropLeaderboardStorage(null, EliteLeaderboardMode.ALL_TIME)

            @Expose
            var pestDisplayType: PestLeaderboardStorage = PestLeaderboardStorage(null, EliteLeaderboardMode.ALL_TIME)

            @Expose
            var weightDisplayType: WeightLeaderboardStorage =
                WeightLeaderboardStorage(FarmingWeight.FARMING_WEIGHT, EliteLeaderboardMode.ALL_TIME)

            @Expose
            val minAmountMap: MutableMap<EliteLeaderboardType, Double> = mutableMapOf()

        }

        @Expose
        val npcVisitorLocations: MutableMap<String, LorenzVec> = mutableMapOf()

        @Expose
        val customGoalMilestone: MutableMap<CropType, Int> = enumMapOf()

        @Expose
        var pestProfitTracker: PestProfitTracker.BucketData = PestProfitTracker.BucketData()

        @Expose
        var activeVinyl: VinylType? = null

        @Expose
        var gardenBpsTracker: GardenBpsTracker.TimedData = GardenBpsTracker.TimedData()

        @Expose
        val overflowHoeLevels: MutableMap<String, Int> = mutableMapOf()

        @Expose
        var cropFeverTracker: CropFeverTracker.BucketData = CropFeverTracker.BucketData()

        @Expose
        var greenhouse: GreenHouseStorage = GreenHouseStorage()

        class GreenHouseStorage(
            @Expose var nextCycle: SimpleTimeMark = farPast(),
        )
    }

    // - gui
    @Expose
    var beaconPower: BeaconPowerStorage = BeaconPowerStorage()

    class BeaconPowerStorage {
        @Expose
        var beaconPowerExpiryTime: SimpleTimeMark? = null

        @Expose
        var boostedStat: Component? = null
    }

    // - inventory
    @Expose
    var experimentation: ExperimentationStorage = ExperimentationStorage()

    class ExperimentationStorage {
        @Expose
        var tablePos: LorenzVec = LorenzVec()

        @Expose
        var dryStreak: ExperimentsDryStreakStorage = ExperimentsDryStreakStorage()

        class ExperimentsDryStreakStorage {
            @Expose
            var attemptsSince: Int = 0

            @Expose
            var xpSince: Long = 0
        }

        @Expose
        var experimentsProfitTracker: ExperimentsProfitTracker.Data = ExperimentsProfitTracker.Data()
    }

    @Expose
    var cakeData: CakeData = CakeData()

    data class CakeData(
        @Expose var ownedCakes: MutableSet<Int> = mutableSetOf(),
        @Expose var missingCakes: MutableSet<Int> = mutableSetOf(),
    )

    @Expose
    var wardrobe: WardrobeStorage = WardrobeStorage()

    @Expose
    var equipmentWardrobe: WardrobeStorage = WardrobeStorage()

    class WardrobeStorage {
        @Expose
        val data: MutableMap<Int, WardrobeData> = mutableMapOf()

        @Expose
        var currentSlot: Int? = null
    }

    @Expose
    var loadout: LoadoutStorage = LoadoutStorage()

    class LoadoutStorage {
        @Expose
        val data: MutableMap<Int, LoadoutData> = mutableMapOf()

        @Expose
        var currentSlot: Int? = null
    }

    @Expose
    var equipment: EquipmentStorage = EquipmentStorage()

    class EquipmentStorage {
        @Expose
        val slots: MutableList<SafeItemStack?> = CurrentEquipmentApi.getEmptyEquipment()

        @Expose
        val riftSlots: MutableList<SafeItemStack?> = CurrentEquipmentApi.getEmptyEquipment()
    }

    @Expose
    var bazaarOrders: BazaarOrdersStorage = BazaarOrdersStorage()

    class BazaarOrdersStorage {
        @Expose
        val buyOrders: MutableMap<NeuInternalName, Int> = mutableMapOf()

        @Expose
        val sellOffers: MutableMap<NeuInternalName, Int> = mutableMapOf()
    }

    // - foraging
    @Expose
    val foraging: ForagingStorage = ForagingStorage()

    class ForagingStorage {
        @Expose
        var hotFTree: HotxTree = HotxTree()

        @Expose
        var tokens: Int = 0

        @Expose
        var availableTokens: Int = 0

        @Expose
        var whispers: PowderStorage = PowderStorage()

        // todo when we're fully 1.21, change ForagingTrackerLegacy to ForagingTracker
        @Expose
        var trackerData: ForagingTrackerLegacy.BucketData = ForagingTrackerLegacy.BucketData()

        @Expose
        var honeyhiveRemindTime: SimpleTimeMark = farPast()
    }

    // - mining
    @Expose
    var mining: MiningStorage = MiningStorage()

    class PowderStorage {
        @Expose
        var available: Long? = null

        @Expose
        var total: Long? = null
    }

    class MiningStorage {
        @Expose
        val kingsTalkedTo: MutableList<String> = mutableListOf()

        @Expose
        var fossilExcavatorProfitTracker: ExcavatorProfitTracker.Data = ExcavatorProfitTracker.Data()

        @Expose
        var hotmTree: HotxTree = HotxTree()

        @Expose
        val powder: MutableMap<PowderType, PowderStorage> = enumMapOf()

        @Expose
        var tokens: Int = 0

        @Expose
        var availableTokens: Int = 0

        @Expose
        var mineshaft: MineshaftStorage = MineshaftStorage()

        class MineshaftStorage {
            @Expose
            var mineshaftTotalBlocks: Long = 0L

            @Expose
            var mineshaftTotalCount: Int = 0

            @Expose
            val blocksBroken: MutableList<PityData> = mutableListOf()

            @Expose
            var corpseProfitTracker: CorpseTracker.BucketData = CorpseTracker.BucketData()

            @Expose
            val mineshaftsEnteredSinceNew: MutableMap<MineshaftDetection.MineshaftType, Int> = mutableMapOf()

            @Expose
            val lastMineshaftTimeNew: MutableMap<MineshaftDetection.MineshaftType, SimpleTimeMark> = mutableMapOf()
        }

        @Expose
        var crystalNucleusTracker: CrystalNucleusTracker.Data = CrystalNucleusTracker.Data()

        @Expose
        var flowstatePersonalBest = 0

        @Expose
        var darkMonolithTracker: DarkMonolithFeatures.Data = DarkMonolithFeatures.Data()
    }

    @Expose
    var powderTracker: PowderTracker.Data = PowderTracker.Data()

    // - minion
    @Expose
    val minions: MutableMap<LorenzVec, MinionConfig>? = mutableMapOf()

    class MinionConfig {
        @Expose
        var displayName: String = ""

        @Expose
        var lastClicked: SimpleTimeMark = farPast()

        override fun toString(): String {
            return "MinionConfig{" +
                "displayName='$displayName'" +
                ", lastClicked=$lastClicked" +
                "}"
        }
    }

    @Expose
    var infernoMinionProfitTracker: InfernoMinionProfitTracker.Data = InfernoMinionProfitTracker.Data()

    // - misc
    @Expose
    var trapperData: TrapperData = TrapperData()

    class TrapperData {
        @Expose
        var questsDone: Int = 0

        @Expose
        var peltsGained: Int = 0

        @Expose
        var killedAnimals: Int = 0

        @Expose
        var selfKillingAnimals: Int = 0

        // TODO change to sh tracker
        @Expose
        var animalRarities: Map<TrapperMobRarity, Int> = enumMapOf()
    }

    @Expose
    var draconicSacrificeTracker: DraconicSacrificeTracker.Data = DraconicSacrificeTracker.Data()

    @Expose
    var abiphoneContactAmount: Int? = null

    @Expose
    val enchantedClockBoosts: MutableMap<EnchantedClockHelper.SimpleBoostType, EnchantedClockHelper.Status> = enumMapOf()

    @Expose
    var npcDayLimit: NpcDayLimitStorage = NpcDayLimitStorage()

    class NpcDayLimitStorage {
        @Expose
        var gmtEpochDay: Long = 0L

        @Expose
        var soldCoins: Long = 0L
    }

    // - nether
    @Expose
    var crimsonIsle: CrimsonIsleStorage = CrimsonIsleStorage()

    class CrimsonIsleStorage {
        @Expose
        val quests: MutableList<String> = mutableListOf()

        @Expose
        val miniBossesDoneToday: MutableList<String> = mutableListOf()

        @Expose
        val kuudraTiersDone: MutableList<String> = mutableListOf()

        @Expose
        val trophyFishes: MutableMap<String, MutableMap<TrophyRarity, Int>> = mutableMapOf()

        @Expose
        val reputation: MutableMap<FactionType, Int> = mutableMapOf()
    }

    // - rift
    @Expose
    var rift: RiftStorage = RiftStorage()

    class RiftStorage {
        @Expose
        val completedKloonTerminals: MutableList<KloonTerminal> = mutableListOf()

        @Expose
        var verminTracker: VerminTracker.Data = VerminTracker.Data()

        @Expose
        var timiteTracker: TimiteTracker.Data = TimiteTracker.Data()

        @Expose
        var ubikRemindTime: SimpleTimeMark = farFuture()
    }

    // - slayer
    @Expose
    val slayerProfitData: MutableMap<String, SlayerProfitTracker.Data> = mutableMapOf()

    @Expose
    val slayerRngMeter: MutableMap<String, SlayerRngMeterStorage> = mutableMapOf()

    data class SlayerRngMeterStorage(
        @Expose var currentMeter: Long = -1,
        @Expose var gainPerBoss: Long = -1,
        @Expose var goalNeeded: Long = -1,
        @Expose var itemGoal: String = "?",
    )

    // data
    @Expose
    var currentPetUuid: UUID? = null

    @Expose
    val stats: MutableMap<SkyblockStat, Double?> = enumMapOf()

    @Expose
    var maxwell: MaxwellPowerStorage = MaxwellPowerStorage()

    class MaxwellPowerStorage {
        @Expose
        var currentPower: String? = null

        @Expose
        var magicalPower: Int = -1

        @Expose
        var tunings: List<ThaumaturgyPowerTuning> = listOf()

        @Expose
        var favoritePowers: List<String> = listOf()
    }

    @Expose
    var arrows: ArrowsStorage = ArrowsStorage()

    class ArrowsStorage {
        @Expose
        var currentArrow: String? = null

        @Expose
        val arrowAmount: MutableMap<NeuInternalName, Int> = mutableMapOf()
    }

    @Expose
    var bits: BitsStorage = BitsStorage()

    class BitsStorage {
        @Expose
        var bits: Int = -1

        @Expose
        var bitsAvailable: Int = -1

        @Expose
        var boosterCookieExpiryTime: SimpleTimeMark? = null

        @Expose
        var museumMilestone: Int? = null
    }

    @Expose
    var godPotExpiry: SimpleTimeMark = farPast()

    @Expose
    var fairySouls: FairySoulsStorage = FairySoulsStorage()

    class FairySoulsStorage {
        @Expose
        val totalFound: MutableMap<IslandType, Int> = mutableMapOf()

        @Expose
        val found: MutableMap<IslandType, MutableSet<LorenzVec>> = mutableMapOf()
    }

    @Expose
    var spider: SpiderStorage = SpiderStorage()

    class SpiderStorage {
        @Expose
        var relics: SpiderRelicsStorage = SpiderRelicsStorage()

        class SpiderRelicsStorage {
            @Expose
            val found: MutableSet<LorenzVec> = mutableSetOf()
        }
    }

    @Expose
    var cakeCounterData: CakeCounterData = CakeCounterData()

    class CakeCounterData(
        @Expose var cakesEaten: Int? = -1,
        @Expose var soulsFound: Int = 0,
    )

    @Expose
    val attributeShards: MutableMap<String, AttributeShardData> = mutableMapOf()

    data class AttributeShardData(
        @Expose var amountSyphoned: Int = 0,
        @Expose var amountInBox: Int = 0,
        @Expose var enabled: Boolean = true,
    )

    // - hunting
    @Expose
    var hunting: HuntingStorage = HuntingStorage()

    class HuntingStorage {
        @Expose
        val trackedAttributeShards: MutableMap<String, Int> = mutableMapOf()

        @Expose
        var huntingProfitTracker: HuntingProfitTracker.Data = HuntingProfitTracker.Data()
    }

    @Expose
    val hiddenCoopMembers: MutableSet<String> = mutableSetOf()
}
