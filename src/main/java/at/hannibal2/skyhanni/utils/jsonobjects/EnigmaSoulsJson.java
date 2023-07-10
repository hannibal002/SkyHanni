package at.hannibal2.skyhanni.utils.jsonobjects;

import at.hannibal2.skyhanni.utils.LorenzVec;

import java.util.List;
import java.util.Map;

public class EnigmaSoulsJson {
    public Map<String, List<EnigmaPosition>> areas;

    public static class EnigmaPosition {
        public String name;
        public LorenzVec position;
    }
}