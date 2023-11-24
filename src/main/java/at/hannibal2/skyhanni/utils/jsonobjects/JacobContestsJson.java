package at.hannibal2.skyhanni.utils.jsonobjects;

import at.hannibal2.skyhanni.features.garden.CropType;
import com.google.gson.annotations.Expose;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JacobContestsJson {
    @Expose
    public Map<Long, List<CropType>> contestTimes = new HashMap<>();
}
