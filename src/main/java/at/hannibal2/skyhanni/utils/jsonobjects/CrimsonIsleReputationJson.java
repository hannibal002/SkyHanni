package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class CrimsonIsleReputationJson {
    @Expose
    public Map<String, ReputationQuest> FISHING;

    @Expose
    public Map<String, ReputationQuest> RESCUE;

    @Expose
    public Map<String, ReputationQuest> FETCH;

    @Expose
    public Map<String, ReputationQuest> DOJO;

    @Expose
    public Map<String, ReputationQuest> MINIBOSS;

    @Expose
    public Map<String, ReputationQuest> KUUDRA;

    public static class ReputationQuest {
        @Expose
        public String item;

        @Expose
        public List<Double> location;
    }
}
