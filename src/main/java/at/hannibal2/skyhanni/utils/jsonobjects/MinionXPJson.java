package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.Map;

public class MinionXPJson {
    @Expose
    public Map<String, Map<String, Double>> minion_xp;
}
