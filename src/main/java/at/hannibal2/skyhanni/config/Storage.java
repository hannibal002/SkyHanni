package at.hannibal2.skyhanni.config;

import at.hannibal2.skyhanni.data.model.ComposterUpgrade;
import at.hannibal2.skyhanni.features.garden.CropAccessory;
import at.hannibal2.skyhanni.features.garden.CropType;
import at.hannibal2.skyhanni.features.garden.fortuneguide.FarmingItems;
import at.hannibal2.skyhanni.features.garden.visitor.VisitorReward;
import at.hannibal2.skyhanni.features.misc.FrozenTreasure;
import at.hannibal2.skyhanni.features.misc.GhostCounter.Option;
import at.hannibal2.skyhanni.features.rift.area.westvillage.KloonTerminal;
import at.hannibal2.skyhanni.utils.LorenzVec;
import net.minecraft.item.ItemStack;

import java.util.*;

public class Storage {

    public Map<Long, List<CropType>> gardenJacobFarmingContestTimes = new HashMap<>();

    public String apiKey = "";

    public Map<UUID, PlayerSpecific> players = new HashMap<>();

    public static class PlayerSpecific {

        public Map<String, ProfileSpecific> profiles = new HashMap<>(); // profile name

        public Integer gardenCommunityUpgrade = -1;

        public long nextCityProjectParticipationTime = 0L;
    }

    public static class ProfileSpecific {

        public String currentPet = "";

        public Map<LorenzVec, MinionConfig> minions = new HashMap<>();

        public static class MinionConfig {

            public String displayName = "";

            public long lastClicked = -1;

        }

        public CrimsonIsleStorage crimsonIsle = new CrimsonIsleStorage();

        public static class CrimsonIsleStorage {

            public List<String> quests = new ArrayList<>();

            public int latestTrophyFishInInventory = 0;

            public List<String> miniBossesDoneToday = new ArrayList<>();

            public List<String> kuudraTiersDone = new ArrayList<>();
        }

        public ProfileSpecific.GardenStorage garden = new ProfileSpecific.GardenStorage();

        public static class GardenStorage {

            public int experience = -1;

            public Map<CropType, Long> cropCounter = new HashMap<>();

            public Map<CropType, Integer> cropUpgrades = new HashMap<>();

            public Map<CropType, Integer> cropsPerSecond = new HashMap<>();

            public Map<CropType, Double> latestBlocksPerSecond = new HashMap<>();

            public Map<CropType, Double> latestTrueFarmingFortune = new HashMap<>();

            public CropAccessory savedCropAccessory = null;

            public Map<String, Integer> dicerRngDrops = new HashMap<>();

            public long informedAboutLowMatter = 0;

            public long informedAboutLowFuel = 0;

            public long visitorInterval = 15 * 60_000L;

            public long nextSixthVisitorArrival = 0;

            public Map<String, Integer> farmArmorDrops = new HashMap<>();

            public Map<ComposterUpgrade, Integer> composterUpgrades = new HashMap<>();

            public Map<CropType, Boolean> toolWithBountiful = new HashMap<>();

            public String composterCurrentOrganicMatterItem = "";

            public String composterCurrentFuelItem = "";

            public int uniqueVisitors = 0;

            public VisitorDrops visitorDrops = new VisitorDrops();

            public static class VisitorDrops {
                public int acceptedVisitors = 0;

                public int deniedVisitors = 0;

                public List<Long> visitorRarities = new ArrayList<>();

                public int copper = 0;

                public long farmingExp = 0;

                public int gardenExp = 0;

                public long coinsSpent = 0;

                public Map<VisitorReward, Integer> rewardsCount = new HashMap<>();
            }

            public PlotIcon plotIcon = new PlotIcon();

            public static class PlotIcon {
                public Map<Integer, String> plotList = new HashMap<>();
            }

            public Map<CropType, LorenzVec> cropStartLocations = new HashMap<>();

            public Fortune fortune = new Fortune();

            public static class Fortune {

                public Map<FarmingItems, Boolean> outdatedItems = new HashMap<>();

                public int anitaUpgrade = -1;

                public int farmingStrength = -1;

                public int farmingLevel = -1;

                public int plotsUnlocked = -1;

                public long cakeExpiring = -1L;

                public boolean carrotFortune = false;

                public Map<FarmingItems, ItemStack> farmingItems = new HashMap<>();
            }

            public long composterEmptyTime = 0;

            public long lastComposterEmptyWarningTime = 0;
        }

        public GhostCounter ghostCounter = new GhostCounter();

        public static class GhostCounter {

            public Map<Option, Double> data = new HashMap<>();

            public boolean ctDataImported = false;

            public double bestiaryNextLevel = 0;

            public double bestiaryCurrentKill = 0;

            public double bestiaryKillNeeded = 0;

            public double totalMF = 0;

        }

        public FrozenTreasureTracker frozenTreasureTracker = new FrozenTreasureTracker();

        public static class FrozenTreasureTracker {
            public int treasuresMined = 0;

            public int compactProcs = 0;

            public Map<FrozenTreasure, Integer> treasureCount = new HashMap<>();
        }

        public RiftStorage rift = new RiftStorage();

        public static class RiftStorage {

            public List<KloonTerminal> completedKloonTerminals = new ArrayList<>();

        }

        public Map<String, SlayerProfitList> slayerProfitData = new HashMap<>();

        public static class SlayerProfitList {

            public Map<String, SlayerItemProfit> items = new HashMap<>();

            public long mobKillCoins = 0;

            public long slayerSpawnCost = 0;

            public int slayerCompletedCount = 0;

            public static class SlayerItemProfit {
                public String internalName;
                public long timesDropped;
                public long totalAmount;
                public boolean hidden;
            }
        }

        public Map<String, SlayerRngMeterStorage> slayerRngMeter = new HashMap<>();

        public static class SlayerRngMeterStorage {

            public long currentMeter = -1;

            public long gainPerBoss = -1;

            public long goalNeeded = -1;

            public String itemGoal = "?";
        }
    }
}