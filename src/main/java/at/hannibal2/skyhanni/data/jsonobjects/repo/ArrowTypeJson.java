package at.hannibal2.skyhanni.data.jsonobjects.repo;

import com.google.gson.annotations.Expose;

import java.util.Map;

public class ArrowTypeJson {
    @Expose
    public Map<String, ArrowAttributes> arrows;

    public static class ArrowAttributes {
        @Expose
        public String arrow;
    }
}
