package at.hannibal2.skyhanni.utils.jsonobjects;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModsJson {

    public Map<String, Mod> mods = new HashMap<>();

    public static class Mod {
        public List<String> description = new ArrayList<>();

        public String command = "";

        public List<String> guiPath = new ArrayList<>();
    }
}
