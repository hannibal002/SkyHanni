package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.data.model.ComposterUpgrade;
import at.hannibal2.skyhanni.features.garden.CropAccessory;
import at.hannibal2.skyhanni.features.garden.CropType;
import at.hannibal2.skyhanni.features.garden.visitor.VisitorReward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OldHidden {

    public String apiKey = "";

    public String currentPet = "";

    public Map<String, Long> minionLastClick = new HashMap<>();

    public Map<String, String> minionName = new HashMap<>();

    public List<String> crimsonIsleQuests = new ArrayList<>();

    public int crimsonIsleLatestTrophyFishInInventory = 0;

    public List<String> crimsonIsleMiniBossesDoneToday = new ArrayList<>();

    public List<String> crimsonIsleKuudraTiersDone = new ArrayList<>();

    public Map<CropType, Long> gardenCropCounter = new HashMap<>();

    public Map<CropType, Integer> gardenCropUpgrades = new HashMap<>();

    public Map<CropType, Integer> gardenCropsPerSecond = new HashMap<>();

    public Map<CropType, Double> gardenLatestBlocksPerSecond = new HashMap<>();

    public Map<CropType, Double> gardenLatestTrueFarmingFortune = new HashMap<>();

    public int gardenExp = -1;

    public CropAccessory savedCropAccessory = null;

    public Map<String, Integer> gardenDicerRngDrops = new HashMap<>();

    public long informedAboutLowMatter = 0;

    public long informedAboutLowFuel = 0;

    public long visitorInterval = 15 * 60_000L;

    public long nextSixthVisitorArrival = 0;

    public Map<Long, List<CropType>> gardenJacobFarmingContestTimes = new HashMap<>();

    public Map<String, Integer> gardenFarmingArmorDrops = new HashMap<>();

    public Map<ComposterUpgrade, Integer> gardenComposterUpgrades = new HashMap<>();

    public Map<CropType, Boolean> gardenToolHasBountiful = new HashMap<>();

    public String gardenComposterCurrentOrganicMatterItem = "";

    public String gardenComposterCurrentFuelItem = "";


    public VisitorDrops visitorDrops = new VisitorDrops();

    public static class VisitorDrops {
        public int acceptedVisitors = 0;

        public int deniedVisitors = 0;

        public List<Long> visitorRarities = new ArrayList<>();

        public int copper = 0;

        public long farmingExp = 0;

        public long coinsSpent = 0;

        public Map<VisitorReward, Integer> rewardsCount = new HashMap<>();
    }

    public boolean isMigrated = false;
}
