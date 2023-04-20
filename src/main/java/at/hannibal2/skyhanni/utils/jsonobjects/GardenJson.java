package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.Map;

public class GardenJson {

    @Expose
    public Map<String, Double> organic_matter;

    @Expose
    public Map<String, Double> fuel;
}
