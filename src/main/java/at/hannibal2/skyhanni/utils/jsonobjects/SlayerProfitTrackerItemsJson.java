package at.hannibal2.skyhanni.utils.jsonobjects;

import at.hannibal2.skyhanni.utils.NEUInternalName;
import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class SlayerProfitTrackerItemsJson {
    @Expose
    public Map<String, List<NEUInternalName>> slayers;
}
