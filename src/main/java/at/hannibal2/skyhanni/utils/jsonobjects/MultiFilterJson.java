package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.List;

public class MultiFilterJson {
    @Expose
    public List<String> equals;

    @Expose
    public List<String> startsWith;

    @Expose
    public List<String> endsWith;

    @Expose
    public List<String> contains;

    @Expose
    public List<String> containsWord;

    @Expose
    public String description;
}