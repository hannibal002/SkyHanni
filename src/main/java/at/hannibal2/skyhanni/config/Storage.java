package at.hannibal2.skyhanni.config;

import at.hannibal2.skyhanni.data.model.ComposterUpgrade;
import at.hannibal2.skyhanni.features.garden.CropAccessory;
import at.hannibal2.skyhanni.features.garden.CropType;
import at.hannibal2.skyhanni.features.garden.visitor.VisitorReward;
import com.google.gson.annotations.Expose;

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

        // TODO save community shop account upgrades here
    }

    public static class ProfileSpecific {

        @Expose
        public String currentPet = "";

        @Expose
        public GardenStorage garden = new GardenStorage();

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
                public long coinsSpent = 0;

                @Expose
                public Map<VisitorReward, Integer> rewardsCount = new HashMap<>();
            }
        }

    }
}
