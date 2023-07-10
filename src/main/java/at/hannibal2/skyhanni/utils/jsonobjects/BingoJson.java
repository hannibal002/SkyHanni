package at.hannibal2.skyhanni.utils.jsonobjects;


import java.util.List;
import java.util.Map;

public class BingoJson {
    public Map<String, BingoTip> bingo_tips;

    public static class BingoTip {
        public String difficulty;
        public List<String> note;
    }
}
