package at.hannibal2.skyhanni.features.misc.massconfiguration;

import com.google.gson.annotations.Expose;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KnownFeaturesJson {
    @Expose
    public Map<String, List<String>> knownFeatures = new HashMap<>();
}
