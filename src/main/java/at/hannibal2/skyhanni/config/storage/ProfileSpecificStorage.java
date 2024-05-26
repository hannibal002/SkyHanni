package at.hannibal2.skyhanni.config.storage;

import at.hannibal2.skyhanni.api.SkillAPI;
import at.hannibal2.skyhanni.data.MaxwellAPI;
import at.hannibal2.skyhanni.data.model.ComposterUpgrade;
import at.hannibal2.skyhanni.features.combat.endernodetracker.EnderNodeTracker;
import at.hannibal2.skyhanni.features.combat.ghostcounter.GhostData;
import at.hannibal2.skyhanni.features.dungeon.CroesusChestTracker;
import at.hannibal2.skyhanni.features.dungeon.DungeonFloor;
import at.hannibal2.skyhanni.features.event.diana.DianaProfitTracker;
import at.hannibal2.skyhanni.features.event.diana.MythologicalCreatureTracker;
import at.hannibal2.skyhanni.features.event.jerry.frozentreasure.FrozenTreasureTracker;
import at.hannibal2.skyhanni.features.fishing.tracker.FishingProfitTracker;
import at.hannibal2.skyhanni.features.fishing.tracker.SeaCreatureTracker;
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity;
import at.hannibal2.skyhanni.features.garden.CropAccessory;
import at.hannibal2.skyhanni.features.garden.CropType;
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI;
import at.hannibal2.skyhanni.features.garden.farming.ArmorDropTracker;
import at.hannibal2.skyhanni.features.garden.farming.DicerRngDropTracker;
import at.hannibal2.skyhanni.features.garden.farming.lane.FarmingLane;
import at.hannibal2.skyhanni.features.garden.fortuneguide.FarmingItems;
import at.hannibal2.skyhanni.features.garden.pests.PestProfitTracker;
import at.hannibal2.skyhanni.features.garden.pests.VinylType;
import at.hannibal2.skyhanni.features.garden.visitor.VisitorReward;
import at.hannibal2.skyhanni.features.mining.fossilexcavator.ExcavatorProfitTracker;
import at.hannibal2.skyhanni.features.mining.powdertracker.PowderTracker;
import at.hannibal2.skyhanni.features.misc.trevor.TrevorTracker;
import at.hannibal2.skyhanni.features.rift.area.westvillage.VerminTracker;
import at.hannibal2.skyhanni.features.rift.area.westvillage.kloon.KloonTerminal;
import at.hannibal2.skyhanni.features.skillprogress.SkillType;
import at.hannibal2.skyhanni.features.slayer.SlayerProfitTracker;
import at.hannibal2.skyhanni.utils.LorenzVec;
import at.hannibal2.skyhanni.utils.NEUInternalName;
import com.google.gson.annotations.Expose;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileSpecificStorage {

    @Expose
    public String currentPet = "";

    @Expose
    public ChocolateFactoryStorage chocolateFactory = new ChocolateFactoryStorage();

    public static class ChocolateFactoryStorage {
        @Expose
        public int currentRabbits = 0;

        @Expose
        public int maxRabbits = -1;

        @Expose
        public long currentChocolate = 0;

        @Expose
        public long chocolateThisPrestige = 0;

        @Expose
        public long chocolateAllTime = 0;

        @Expose
        public int rawChocPerSecond = 0;

        @Expose
        public double chocolateMultiplier = 1.0;

        @Expose
        public double rawChocolateMultiplier = 1.0;

        @Expose
        public int timeTowerLevel = 0;

        @Expose
        public long currentTimeTowerEnds = 0;

        @Expose
        public long nextTimeTower = 0;

        @Expose
        public int currentTimeTowerUses = -1;

        @Expose
        public int timeTowerCooldown = 8;

        @Expose
        public int maxTimeTowerUses = 3;

        @Expose
        public boolean hasMuRabbit = false;

        @Expose
        public long bestUpgradeAvailableAt = 0;

        @Expose
        public long bestUpgradeCost = 0;

        @Expose
        public long lastDataSave = 0;

        @Expose
        public PositionChange positionChange = new PositionChange();

        public static class PositionChange {
            @Expose
            public Long lastTime = null;

            @Expose
            public int lastPosition = -1;

            @Expose
            public String lastLeaderboard = null;
        }

        @Expose
        public Long targetGoal = null;

        @Expose
        public String targetName = null;
    }

    @Expose
    public MaxwellPowerStorage maxwell = new MaxwellPowerStorage();

    public static class MaxwellPowerStorage {
        @Expose
        public String currentPower = null;

        @Expose
        public int magicalPower = -1;

        @Expose
        public List<MaxwellAPI.ThaumaturgyPowerTuning> tunings = new ArrayList<>();
    }

    @Expose
    public ArrowsStorage arrows = new ArrowsStorage();

    public static class ArrowsStorage {
        @Expose
        public String currentArrow = null;

        @Expose
        public Map<NEUInternalName, Integer> arrowAmount = new HashMap<>();
    }

    @Expose
    public BitsStorage bits = new BitsStorage();

    public static class BitsStorage {
        @Expose
        public int bits = -1;

        @Expose
        public int bitsAvailable = -1;

        @Expose
        public Long boosterCookieExpiryTime = null;
    }

    @Expose
    public Map<LorenzVec, MinionConfig> minions = new HashMap<>();

    public static class MinionConfig {

        @Expose
        public String displayName = "";

        @Expose
        public long lastClicked = -1;

        @Override
        public String toString() {
            return "MinionConfig{" +
                "displayName='" + displayName + '\'' +
                ", lastClicked=" + lastClicked +
                '}';
        }
    }

    @Expose
    public CrimsonIsleStorage crimsonIsle = new CrimsonIsleStorage();

    public static class CrimsonIsleStorage {

        @Expose
        public List<String> quests = new ArrayList<>();

        @Expose
        public List<String> miniBossesDoneToday = new ArrayList<>();

        @Expose
        public List<String> kuudraTiersDone = new ArrayList<>();

        @Expose
        public Map<String, Map<TrophyRarity, Integer>> trophyFishes = new HashMap<>();
    }

    @Expose
    public GardenStorage garden = new GardenStorage();

    public static class GardenStorage {

        @Expose
        public Long experience = null;

        @Expose
        public Map<CropType, Long> cropCounter = new HashMap<>();

        @Expose
        public Map<CropType, Integer> cropUpgrades = new HashMap<>();

        @Expose
        public Map<CropType, Integer> cropsPerSecond = new HashMap<>();

        @Expose
        public Map<CropType, Double> latestBlocksPerSecond = new HashMap<>();

        @Expose
        public Map<CropType, Double> latestTrueFarmingFortune = new HashMap<>();

        @Expose
        @Nullable
        public CropAccessory savedCropAccessory = CropAccessory.NONE;

        @Expose
        public DicerRngDropTracker.Data dicerDropTracker = new DicerRngDropTracker.Data();

        @Expose
        public long informedAboutLowMatter = 0;

        @Expose
        public long informedAboutLowFuel = 0;

        @Expose
        public long visitorInterval = 15 * 60_000L;

        @Expose
        public long nextSixthVisitorArrival = 0;

        @Expose
        public ArmorDropTracker.Data armorDropTracker = new ArmorDropTracker.Data();

        @Expose
        public Map<ComposterUpgrade, Integer> composterUpgrades = new HashMap<>();

        @Expose
        public Map<CropType, Boolean> toolWithBountiful = new HashMap<>();

        @Expose
        public NEUInternalName composterCurrentOrganicMatterItem = NEUInternalName.Companion.getNONE();

        @Expose
        public NEUInternalName composterCurrentFuelItem = NEUInternalName.Companion.getNONE();

        @Expose
        public int uniqueVisitors = 0;

        @Expose
        public GardenStorage.VisitorDrops visitorDrops = new GardenStorage.VisitorDrops();

        public static class VisitorDrops {
            @Expose
            public int acceptedVisitors = 0;

            @Expose
            public int deniedVisitors = 0;

            @Expose
            public List<Long> visitorRarities = new ArrayList<>();

            @Expose
            public int copper = 0;

            @Expose
            public long farmingExp = 0;

            @Expose
            public int gardenExp = 0;

            @Expose
            public long coinsSpent = 0;

            @Expose
            public long bits = 0;

            @Expose
            public long mithrilPowder = 0;

            @Expose
            public long gemstonePowder = 0;

            @Expose
            public Map<VisitorReward, Integer> rewardsCount = new HashMap<>();
        }

        @Expose
        public GardenStorage.PlotIcon plotIcon = new GardenStorage.PlotIcon();

        public static class PlotIcon {
            @Expose
            public Map<Integer, NEUInternalName> plotList = new HashMap<>();
        }

        @Expose
        public Map<Integer, GardenPlotAPI.PlotData> plotData = new HashMap<>();

        @Expose
        public int scoreboardPests = 0;

        @Expose
        public Map<CropType, LorenzVec> cropStartLocations = new HashMap<>();

        @Expose
        public Map<CropType, FarmingLane> farmingLanes = new HashMap<>();

        @Expose
        public GardenStorage.Fortune fortune = new GardenStorage.Fortune();

        public static class Fortune {

            @Expose
            public Map<FarmingItems, Boolean> outdatedItems = new HashMap<>();

            @Expose
            public int anitaUpgrade = -1;

            @Expose
            public int farmingStrength = -1;

            @Expose
            public int farmingLevel = -1;

            @Expose
            public int plotsUnlocked = -1;

            @Expose
            public long cakeExpiring = -1L;

            @Expose
            public boolean carrotFortune = false;

            @Expose
            public boolean pumpkinFortune = false;

            @Expose
            public boolean cocoaBeansFortune = false;

            @Expose
            public Map<FarmingItems, ItemStack> farmingItems = new HashMap<>();
        }

        @Expose
        public long composterEmptyTime = 0;

        @Expose
        public long lastComposterEmptyWarningTime = 0;

        @Expose
        public GardenStorage.FarmingWeightConfig farmingWeight = new GardenStorage.FarmingWeightConfig();

        public static class FarmingWeightConfig {

            @Expose
            public int lastFarmingWeightLeaderboard = -1;
        }

        @Expose
        public Map<String, LorenzVec> npcVisitorLocations = new HashMap<>();

        @Expose
        public Map<CropType, Integer> customGoalMilestone = new HashMap<>();

        @Expose
        public PestProfitTracker.Data pestProfitTracker = new PestProfitTracker.Data();

        @Expose
        public VinylType activeVinyl = null;
    }

    @Expose
    public GhostCounter ghostCounter = new GhostCounter();

    public static class GhostCounter {

        @Expose
        public Map<GhostData.Option, Double> data = new HashMap<>();

        @Expose
        public boolean ctDataImported = false;

        @Expose
        public double bestiaryNextLevel = 0;

        @Expose
        public double bestiaryCurrentKill = 0;

        @Expose
        public double bestiaryKillNeeded = 0;

        @Expose
        public double totalMF = 0;

        @Expose
        public int configUpdateVersion = 0;

    }

    @Expose
    public PowderTracker.Data powderTracker = new PowderTracker.Data();

    @Expose
    public FrozenTreasureTracker.Data frozenTreasureTracker = new FrozenTreasureTracker.Data();

    @Expose
    public EnderNodeTracker.Data enderNodeTracker = new EnderNodeTracker.Data();

    @Expose
    public RiftStorage rift = new RiftStorage();

    public static class RiftStorage {

        @Expose
        public List<KloonTerminal> completedKloonTerminals = new ArrayList<>();

        @Expose
        public VerminTracker.Data verminTracker = new VerminTracker.Data();

    }

    @Expose
    public Map<String, SlayerProfitTracker.Data> slayerProfitData = new HashMap<>();

    @Expose
    public Map<String, SlayerRngMeterStorage> slayerRngMeter = new HashMap<>();

    public static class SlayerRngMeterStorage {

        @Expose
        public long currentMeter = -1;

        @Expose
        public long gainPerBoss = -1;

        @Expose
        public long goalNeeded = -1;

        @Expose
        public String itemGoal = "?";

        @Override
        public String toString() {
            return "SlayerRngMeterStorage{" +
                "currentMeter=" + currentMeter +
                ", gainPerBoss=" + gainPerBoss +
                ", goalNeeded=" + goalNeeded +
                ", itemGoal='" + itemGoal + '\'' +
                '}';
        }
    }

    @Expose
    public MiningConfig mining = new MiningConfig();

    public static class MiningConfig {

        @Expose
        public List<String> kingsTalkedTo = new ArrayList<>();

        @Expose
        public ExcavatorProfitTracker.Data fossilExcavatorProfitTracker = new ExcavatorProfitTracker.Data();
    }

    @Expose
    public TrapperData trapperData = new TrapperData();

    public static class TrapperData {

        @Expose
        public int questsDone;

        @Expose
        public int peltsGained;

        @Expose
        public int killedAnimals;

        @Expose
        public int selfKillingAnimals;

        @Expose
        public Map<TrevorTracker.TrapperMobRarity, Integer> animalRarities = new HashMap<>();
    }

    @Expose
    public DungeonStorage dungeons = new DungeonStorage();

    public static class DungeonStorage {

        @Expose
        public Map<DungeonFloor, Integer> bosses = new HashMap<>();

        @Expose
        public List<DungeonStorage.DungeonRunInfo> runs = CroesusChestTracker.Companion.generateMaxChestAsList();

        public static class DungeonRunInfo {

            public DungeonRunInfo() {
            }

            public DungeonRunInfo(String floor) {
                this.floor = floor;
                this.openState = CroesusChestTracker.OpenedState.UNOPENED;
            }

            @Nullable
            @Expose
            public String floor = null;

            @Expose
            @Nullable
            public CroesusChestTracker.OpenedState openState = null;

            @Expose
            @Nullable
            public Boolean kismetUsed = null;

        }
    }

    @Expose
    public FishingStorage fishing = new FishingStorage();

    public static class FishingStorage {

        @Expose
        public FishingProfitTracker.Data fishingProfitTracker = new FishingProfitTracker.Data();

        @Expose
        public SeaCreatureTracker.Data seaCreatureTracker = new SeaCreatureTracker.Data();

    }

    @Expose
    public DianaStorage diana = new DianaStorage();

    public static class DianaStorage {

        @Expose
        // TODO rename to 'profitTracker'
        public DianaProfitTracker.Data dianaProfitTracker = new DianaProfitTracker.Data();

        @Expose
        // TODO renmae
        public MythologicalCreatureTracker.Data mythologicalMobTracker = new MythologicalCreatureTracker.Data();
    }

    @Expose
    public Map<SkillType, SkillAPI.SkillInfo> skillData = new HashMap<>();
}
