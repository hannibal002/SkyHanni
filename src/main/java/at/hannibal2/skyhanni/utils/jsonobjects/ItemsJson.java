package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class ItemsJson {
    @Expose
    public List<String> crimson_armors;

    @Expose
    public Map<String, Integer> crimson_tiers;
}