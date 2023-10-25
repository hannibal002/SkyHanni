package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.List;

public class HideNotClickableItemsJson {
    @Expose
    public MultiFilterJson hide_npc_sell;

    @Expose
    public MultiFilterJson hide_in_storage;

    @Expose
    public MultiFilterJson hide_player_trade;

    @Expose
    public MultiFilterJson not_auctionable;

    @Expose
    public SalvageFilter salvage;

    public static class SalvageFilter {
        @Expose
        public List<String> armor;

        @Expose
        public List<String> items;
    }
}