package at.hannibal2.skyhanni.config.features.garden.moneyperhour;

import at.hannibal2.skyhanni.config.HasLegacyId;

public enum CustomFormatEntry implements HasLegacyId {
    SELL_OFFER("§eSell Offer", 0),
    INSTANT_SELL("§eInstant Sell", 1),
    NPC_PRICE("§eNPC Price", 2),
    ;

    private final String str;
    private final int legacyId;

    CustomFormatEntry(String str, int legacyId) {
        this.str = str;
        this.legacyId = legacyId;
    }

    // Constructor if new enum elements are added post-migration
    CustomFormatEntry(String str) {
        this(str, -1);
    }

    @Override
    public int getLegacyId() {
        return legacyId;
    }

    @Override
    public String toString() {
        return str;
    }
}
