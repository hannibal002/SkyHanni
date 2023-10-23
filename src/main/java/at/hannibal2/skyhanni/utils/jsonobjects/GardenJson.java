package at.hannibal2.skyhanni.utils.jsonobjects;

import at.hannibal2.skyhanni.features.garden.CropType;
import at.hannibal2.skyhanni.utils.NEUInternalName;
import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class GardenJson {
    @Expose
    public List<Integer> garden_exp;

    @Expose
    public Map<CropType, List<Integer>> crop_milestones;

    @Expose
    public Map<String, GardenVisitor> visitors;

    @Expose
    public Map<NEUInternalName, Double> organic_matter;

    @Expose
    public Map<NEUInternalName, Double> fuel;

    public static class GardenVisitor {
        @Expose
        public String rarity;

        @Expose
        public List<String> need_items;
    }
}
