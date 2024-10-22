package at.hannibal2.skyhanni.data.jsonobjects.repo;

import at.hannibal2.skyhanni.utils.NEUInternalName;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GhostDrops {
    @Expose @SerializedName("ghost_drops")
    public List<NEUInternalName> ghostDrops;
}
