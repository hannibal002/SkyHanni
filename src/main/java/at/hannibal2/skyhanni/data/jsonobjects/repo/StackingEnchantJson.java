package at.hannibal2.skyhanni.data.jsonobjects.repo;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class StackingEnchantJson {
    @Expose
    public Map<String, StackingEnchantData> enchants;

    public static class StackingEnchantData {
        @Expose
        public List<Integer> levels;

        @Expose
        public String statName;
    }
}
