package at.hannibal2.skyhanni.data.jsonobjects.repo;

import at.hannibal2.skyhanni.utils.LorenzVec;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

public class ParkourJson {
    @Expose
    public List<LorenzVec> locations;

    @Expose
    public List<ShortCut> shortCuts = new ArrayList<>();

    public static class ShortCut {
        @Expose
        public int from;

        @Expose
        public int to;
    }
}
