package at.hannibal2.skyhanni.config;

import at.hannibal2.skyhanni.data.model.ComposterUpgrade;
import at.hannibal2.skyhanni.features.garden.CropAccessory;
import at.hannibal2.skyhanni.features.garden.CropType;
import at.hannibal2.skyhanni.features.garden.fortuneguide.FarmingItems;
import at.hannibal2.skyhanni.features.garden.visitor.VisitorReward;
import at.hannibal2.skyhanni.utils.LorenzVec;
import com.google.gson.annotations.Expose;
import net.minecraft.item.ItemStack;

import java.util.*;

public class Storage {

    @Expose
    public Map<Long, List<CropType>> gardenJacobFarmingContestTimes = new HashMap<>();

    @Expose
    public String apiKey = "";

    @Expose
    public Map<UUID, PlayerSpecific> players = new HashMap<>();

    public static class PlayerSpecific {

        @Expose
        public Map<String, ProfileSpecific> profiles = new HashMap<>(); // profile name

        @Expose
        public Integer gardenCommunityUpgrade = -1;
    }

    public static class ProfileSpecific {

        @Expose
        public String currentPet = "";

        @Expose
        public Map<LorenzVec, MinionConfig> minions = new HashMap<>();

        public static class MinionConfig {

            @Expose
            public String displayName = "";

            @Expose
            public long lastClicked = -1;

        }

        @Expose
        public CrimsonIsleStorage crimsonIsle = new CrimsonIsleStorage();

        public static class CrimsonIsleStorage {

            @Expose
            public List<String> quests = new ArrayList<>();

            @Expose
            public int latestTrophyFishInInventory = 0;

            @Expose
            public List<String> miniBossesDoneToday = new ArrayList<>();

            @Expose
            public List<String> kuudraTiersDone = new ArrayList<>();
        }

        @Expose
        public ProfileSpecific.GardenStorage garden = new ProfileSpecific.GardenStorage();

        public static class GardenStorage {

            @Expose
            public int experience = -1;

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
            public CropAccessory savedCropAccessory = null;

            @Expose
            public Map<String, Integer> dicerRngDrops = new HashMap<>();

            @Expose
            public long informedAboutLowMatter = 0;

            @Expose
            public long informedAboutLowFuel = 0;

            @Expose
            public long visitorInterval = 15 * 60_000L;

            @Expose
            public long nextSixthVisitorArrival = 0;

            @Expose
            public Map<String, Integer> farmArmorDrops = new HashMap<>();

            @Expose
            public Map<ComposterUpgrade, Integer> composterUpgrades = new HashMap<>();

            @Expose
            public Map<CropType, Boolean> toolWithBountiful = new HashMap<>();

            @Expose
            public String composterCurrentOrganicMatterItem = "";

            @Expose
            public String composterCurrentFuelItem = "";

            @Expose
            public VisitorDrops visitorDrops = new VisitorDrops();

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
                public Map<VisitorReward, Integer> rewardsCount = new HashMap<>();
            }

            @Expose
            public PlotIcon plotIcon = new PlotIcon();

            public static class PlotIcon {
                @Expose
                public Map<Integer, String> plotList = new HashMap<>();
            }

            @Expose
            public Map<CropType, LorenzVec> cropStartLocations = new HashMap<>();

            @Expose
            public Fortune fortune = new Fortune();

            public static class Fortune {

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
                public Map<FarmingItems, ItemStack> farmingItems = new HashMap<>();
            }
        }

        @Expose
        public long nextCityProjectParticipationTime = 0L;

        @Expose
        public Map<String, SlayerProfitList> slayerProfitData = new HashMap<>();

        public static class SlayerProfitList {

            @Expose
            public Map<String, SlayerItemProfit> items = new HashMap<>();

            @Expose
            public long mobKillCoins = 0;

            @Expose
            public long slayerSpawnCost = 0;

            @Expose
            public int slayerCompletedCount = 0;

            public static class SlayerItemProfit {
                @Expose
                public String internalName;
                @Expose
                public long timesDropped;
                @Expose
                public long totalAmount;
                @Expose
                public boolean hidden;
            }
        }

    }
}