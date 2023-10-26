package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DicerDropsJson {
    @Expose
    public DicerType MELON;

    @Expose
    public DicerType PUMPKIN;

    public static class DicerType {
        @Expose
        @SerializedName("total chance")
        public Integer totalChance;

        @Expose
        public List<DropInfo> drops;
    }

    public static class DropInfo {
        @Expose
        public Integer chance;

        @Expose
        public List<Integer> amount;
    }
}