package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class ArmorDropsJson {
    @Expose
    public Map<String, DropInfo> specialCrops;

    public static class DropInfo {
        @Expose
        public String armorType;
        @Expose
        public List<Integer> chances;
    }
}