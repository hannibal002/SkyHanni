package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class ArmorDropsJson {
    @Expose
    public Map<String, DropInfo> special_crops;

    public static class DropInfo {
        @Expose
        public String armor_type;

        @Expose
        public List<Double> chance;
    }
}