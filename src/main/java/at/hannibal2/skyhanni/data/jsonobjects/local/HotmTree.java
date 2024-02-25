package at.hannibal2.skyhanni.data.jsonobjects.local;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import java.util.HashMap;
import java.util.Map;

public class HotmTree {

    @Expose
    public Map<String, HotmPerk> perks = new HashMap<>();

    public HotmTree deepCopy() {
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return gson.fromJson(json, HotmTree.class);
    }

    public static class HotmPerk {

        @Expose
        public int level = 0;

        @Expose
        public boolean enabled = false;

        @Expose
        public boolean isUnlocked = false;
    }
}
