package at.hannibal2.skyhanni.data.jsonobjects.repo;

import at.hannibal2.skyhanni.utils.LorenzVec;
import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class EventWaypointsJson {

    @Expose
    public Map<String, List<Waypoint>> presents;

    @Expose
    public Map<String, List<Waypoint>> presents_entrances;

    public static class Waypoint {
        @Expose
        public String name;

        //format: "x:y:z"
        @Expose
        public LorenzVec position;
    }
}
