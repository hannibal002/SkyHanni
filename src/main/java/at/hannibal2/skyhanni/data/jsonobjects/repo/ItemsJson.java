package at.hannibal2.skyhanni.data.jsonobjects.repo;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class ItemsJson {
    @Expose
    public List<String> crimson_armors;

    @Expose
    public Map<String, Integer> crimson_tiers;

    @Expose
    public List<String> lava_fishing_rods;

    @Expose
    public List<String> water_fishing_rods;
}
