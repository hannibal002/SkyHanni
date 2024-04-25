package at.hannibal2.skyhanni.data.jsonobjects.repo;

import com.google.gson.annotations.Expose;

import java.util.Map;

public class DisabledFeaturesJson {
    @Expose
    public Map<String, Boolean> features;
}
