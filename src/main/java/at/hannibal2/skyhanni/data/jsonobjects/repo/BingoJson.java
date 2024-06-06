package at.hannibal2.skyhanni.data.jsonobjects.repo;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class BingoJson {
    @Expose
    public Map<String, BingoData> bingo_tips;

    public static class BingoData {
        @Expose
        public String difficulty;

        @Expose
        public List<String> note;

        @Expose
        public List<String> guide;

        @Expose
        public String found;
    }
}
