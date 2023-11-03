package at.hannibal2.skyhanni.utils.jsonobjects;

import at.hannibal2.skyhanni.features.garden.CropType;
import at.hannibal2.skyhanni.utils.LorenzVec;
import com.google.gson.annotations.Expose;
import org.jetbrains.annotations.Nullable;

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
    public Map<String, Double> organic_matter;

    @Expose
    public Map<String, Double> fuel;

    public static class GardenVisitor {
        @Expose
        public String rarity;

        @Nullable
        @Expose
        public LorenzVec position;

        /**
         * Formatted as follows:
         * - If this visitor is a player, get the encoded skin value
         * - If this visitor is a mob, get their mob class name
         */
        @Nullable
        @Expose
        public String skinOrType;

        @Nullable
        @Expose
        public String mode;

        @Expose
        public List<String> need_items;
    }
}
