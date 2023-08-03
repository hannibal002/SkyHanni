
package at.hannibal2.skyhanni.utils.jsonobjects;
import com.google.gson.annotations.Expose;

import java.util.Map;

public class SeaCreatures {
    @Expose
    public String chat_color;
    @Expose
    public Map<String, SeaCreature> sea_creatures;

    public static class SeaCreature {
        @Expose
        public String chat_message;
        @Expose
        public int fishing_experience;
        @Expose
        public boolean rare;
    }
}