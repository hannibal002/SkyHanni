package at.hannibal2.skyhanni.data.jsonobjects.repo;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModGuiSwitcherJson {

    @Expose
    public Map<String, Mod> mods = new HashMap<>();

    public static class Mod {
        @Expose
        public List<String> description = new ArrayList<>();

        @Expose
        public String command = "";

        @Expose
        public List<String> guiPath = new ArrayList<>();
    }
}
