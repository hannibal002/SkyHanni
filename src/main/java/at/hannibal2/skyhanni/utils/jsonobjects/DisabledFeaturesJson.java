package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.Map;

public class DisabledFeaturesJson {
    @Expose
    public Map<String, Boolean> features;
}