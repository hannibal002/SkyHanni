package at.hannibal2.skyhanni.data.jsonobjects.repo;

import com.google.gson.annotations.Expose;

import java.util.List;

public class SacksJson {

    @Deprecated
    @Expose
    public List<String> sackList;

    @Expose
    public List<String> sackItems;
}
