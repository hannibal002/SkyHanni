package at.hannibal2.skyhanni.utils.jsonobjects;

import at.hannibal2.skyhanni.utils.LorenzVec;
import com.google.gson.annotations.Expose;

import java.util.Map;

public class LocationFixJson {

    @Expose
    public Map<String, LocationFix> locationFixes;

    public static class LocationFix {
        @Expose
        public LorenzVec a;

        @Expose
        public LorenzVec b;

        @Expose
        public String island_name;

        @Expose
        public String real_location;
    }

}
