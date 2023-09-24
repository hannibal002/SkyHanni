package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.data.model.ComposterUpgrade;
import at.hannibal2.skyhanni.features.garden.CropAccessory;
import at.hannibal2.skyhanni.features.garden.CropType;
import at.hannibal2.skyhanni.features.garden.farming.FarmingArmorDrops;
import at.hannibal2.skyhanni.features.garden.visitor.VisitorReward;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OldHidden {

    @Expose
    public String currentPet = "";

    @Expose
    public Map<String, Long> minionLastClick = new HashMap<>();

    @Expose
    public Map<String, String> minionName = new HashMap<>();

    @Expose
    public List<String> crimsonIsleQuests = new ArrayList<>();

    @Expose
    public List<String> crimsonIsleMiniBossesDoneToday = new ArrayList<>();

    @Expose
    public List<String> crimsonIsleKuudraTiersDone = new ArrayList<>();

    @Expose
    public Map<CropType, Long> gardenCropCounter = new HashMap<>();

    @Expose
    public Map<CropType, Integer> gardenCropUpgrades = new HashMap<>();

    @Expose
    public Map<CropType, Integer> gardenCropsPerSecond = new HashMap<>();

    @Expose
    public Map<CropType, Double> gardenLatestBlocksPerSecond = new HashMap<>();

    @Expose
    public Map<CropType, Double> gardenLatestTrueFarmingFortune = new HashMap<>();

    @Expose
    public int gardenExp = -1;

    @Expose
    public CropAccessory savedCropAccessory = null;

    @Expose
    public Map<String, Integer> gardenDicerRngDrops = new HashMap<>();

    @Expose
    public long informedAboutLowMatter = 0;

    @Expose
    public long informedAboutLowFuel = 0;

    @Expose
    public long visitorInterval = 15 * 60_000L;

    @Expose
    public long nextSixthVisitorArrival = 0;

    @Expose
    public Map<Long, List<CropType>> gardenJacobFarmingContestTimes = new HashMap<>();

    @Expose
    public Map<FarmingArmorDrops.ArmorDropType, Integer> gardenFarmingArmorDrops = new HashMap<>();

    @Expose
    public Map<ComposterUpgrade, Integer> gardenComposterUpgrades = new HashMap<>();

    @Expose
    public Map<CropType, Boolean> gardenToolHasBountiful = new HashMap<>();

    @Expose
    public String gardenComposterCurrentOrganicMatterItem = "";

    @Expose
    public String gardenComposterCurrentFuelItem = "";


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

    @Expose
    public boolean isMigrated = false;
}
