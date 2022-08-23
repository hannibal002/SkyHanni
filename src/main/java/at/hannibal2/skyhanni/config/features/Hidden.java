package at.hannibal2.skyhanni.config.features;

import com.google.gson.annotations.Expose;

import java.util.HashMap;
import java.util.Map;

public class Hidden {

    @Expose
    public String apiKey = "";

    @Expose
    public String currentPet = "";

    @Expose
    public Map<String, Long> minions = new HashMap<>();
}
