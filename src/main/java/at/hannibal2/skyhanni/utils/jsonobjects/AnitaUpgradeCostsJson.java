package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.Map;

public class AnitaUpgradeCostsJson {
    @Expose
    public Map<String, Price> level_price;

    public static class Price {
        @Expose
        public Integer gold_medals;
        @Expose
        public Integer jacob_tickets;
    }
}
