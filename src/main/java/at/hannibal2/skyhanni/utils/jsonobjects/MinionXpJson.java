package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.Map;

public class MinionXpJson {
    @Expose
    public Map<String, Xp> minion_xp;

    public static class Xp {
        @Expose
        public String type;

        @Expose
        public Double value;
    }
}
