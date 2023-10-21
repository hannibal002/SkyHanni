package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class BingoJson {
    @Expose
    public Map<String, BingoTip> bingo_tips;

    public static class BingoTip {
        @Expose
        public String difficulty;

        @Expose
        public List<String> note;
    }
}