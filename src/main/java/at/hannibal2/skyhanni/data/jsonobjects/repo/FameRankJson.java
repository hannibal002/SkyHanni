package at.hannibal2.skyhanni.data.jsonobjects.repo;

import com.google.gson.annotations.Expose;

import java.util.Map;

public class FameRankJson {
    @Expose
    public Map<String, FameRank> fame_rank;

    public static class FameRank {
        @Expose
        public String name;

        @Expose
        public int fame_required;

        @Expose
        public double bits_multiplier;

        @Expose
        public int votes;
    }
}
