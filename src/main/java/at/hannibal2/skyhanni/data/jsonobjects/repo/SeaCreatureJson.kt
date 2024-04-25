package at.hannibal2.skyhanni.data.jsonobjects.repo;

import at.hannibal2.skyhanni.utils.LorenzRarity;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class SeaCreatureJson {

    public static Type TYPE = new TypeToken<Map<String, SeaCreatureJson.Variant>>() {
    }.getType();

    public static class Variant {
        @Expose
        public String chat_color;
        @Expose
        public Map<String, SeaCreature> sea_creatures;
    }

    public static class SeaCreature {
        @Expose
        public String chat_message;
        @Expose
        public int fishing_experience;
        @Expose
        public Boolean rare;
        @Expose
        public LorenzRarity rarity;
    }

}
