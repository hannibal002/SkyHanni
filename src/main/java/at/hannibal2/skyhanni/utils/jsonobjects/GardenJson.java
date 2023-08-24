package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class GardenJson {
    @Expose
    public List<Integer> gardenExp;

    @Expose
    public Map<String, List<Integer>> cropMilestones;

    @Expose
    public Map<String, Double> organic_matter;

    @Expose
    public Map<String, Double> fuel;
}