package at.hannibal2.skyhanni.data.jsonobjects.repo;

import com.google.gson.annotations.Expose;
import java.util.List;
import java.util.Map;

public class ReforgesJson {
    @Expose
    public Map<String, ReforgeJson> reforges;

    public static class ReforgeJson {
        @Expose
        public String reforgeStone;

        @Expose
        public String type;

        @Expose
        public List<String> specialItems;

        @Expose
        public String extraProperty;

        @Expose
        public Map<String, Double> customStat;

        @Expose
        public Map<String, Map<String, Double>> stats;
    }
}
