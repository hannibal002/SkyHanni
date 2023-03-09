package at.hannibal2.skyhanni.config.features;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Hidden {

    @Expose
    public String apiKey = "";

    @Expose
    public String currentPet = "";

    @Expose
    public Map<String, Long> minionLastClick = new HashMap<>();

    @Expose
    public Map<String, String> minionName = new HashMap<>();

    @Expose
    public List<String> crimsonIsleQuests = new ArrayList<>();

    @Expose
    public int crimsonIsleLatestTrophyFishInInventory = 0;

    @Expose
    public List<String> crimsonIsleMiniBossesDoneToday = new ArrayList<>();

    @Expose
    public List<String> crimsonIsleKuudraTiersDone = new ArrayList<>();

    @Expose
    public Map<String, Long> gardenCropCounter = new HashMap<>();

    @Expose
    public Map<String, Integer> gardenCropsPerSecond = new HashMap<>();
}
