package at.hannibal2.skyhanni.utils.jsonobjects;

import at.hannibal2.skyhanni.utils.LorenzVec;
import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class EnigmaSoulsJson {
    @Expose
    public Map<String, List<EnigmaPosition>> areas;

    public static class EnigmaPosition {
        @Expose
        public String name;
        @Expose
        public LorenzVec position;
    }
}
