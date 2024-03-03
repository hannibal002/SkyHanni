package at.hannibal2.skyhanni.data.jsonobjects.repo;

import at.hannibal2.skyhanni.utils.NEUInternalName;
import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class ItemsJson {
    @Expose
    public List<String> crimson_armors;

    @Expose
    public Map<String, Integer> crimson_tiers;

    @Expose
    public List<NEUInternalName> lava_fishing_rods;

    @Expose
    public List<NEUInternalName> water_fishing_rods;

    @Expose
    public Map<String, Integer> book_bundle_amount;
}
