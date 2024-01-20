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
    public List<NEUInternalName> trophy_fishing_armors;
}
