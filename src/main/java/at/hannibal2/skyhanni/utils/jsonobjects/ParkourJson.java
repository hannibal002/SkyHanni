package at.hannibal2.skyhanni.utils.jsonobjects;

import at.hannibal2.skyhanni.utils.LorenzVec;

import java.util.ArrayList;
import java.util.List;

public class ParkourJson {
    public List<LorenzVec> locations;
    public List<ShortCut> shortCuts = new ArrayList<>();

    public static class ShortCut {
        public int from;
        public int to;
    }
}
