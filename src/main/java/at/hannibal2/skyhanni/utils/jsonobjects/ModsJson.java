package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModsJson {

    @Expose
    public Map<String, Mod> mods = new HashMap<>();

    public class Mod {
        @Expose
        public List<String> description = new ArrayList<>();

        @Expose
        public String command = "";

        @Expose
        public List<String> guiPath = new ArrayList<>();
    }
}
