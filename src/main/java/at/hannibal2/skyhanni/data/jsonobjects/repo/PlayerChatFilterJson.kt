package at.hannibal2.skyhanni.data.jsonobjects.repo;

import com.google.gson.annotations.Expose;

import java.util.List;

public class PlayerChatFilterJson {
    @Expose
    public List<MultiFilterJson> filters;
}
