package at.hannibal2.skyhanni.utils.jsonobjects;

import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity;
import com.google.gson.annotations.Expose;

import java.util.Map;

public class TrophyFishJson {
    @Expose
    public Map<String, TrophyFishInfo> trophy_fish;

    public static class TrophyFishInfo {
        @Expose
        public String displayName;

        @Expose
        public String description;

        @Expose
        public Integer rate;

        @Expose
        public Map<TrophyRarity, Integer> fillet;
    }
}