package at.hannibal2.skyhanni.data.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class ReforgeStoneJson {

    @Expose
    public String internalName;

    @Expose
    public String reforgeName;

    @Expose
    public String itemTypes;

    @Expose
    public List<String> requiredRarities;

    @Expose
    public Map<String, Map<String, Double>> reforgeStats;

    @Expose
    public Object reforgeAbility;
}
