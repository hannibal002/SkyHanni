package at.hannibal2.skyhanni.config.features.garden.cropmilestones;

import at.hannibal2.skyhanni.config.HasLegacyId;

public enum MilestoneTextEntry implements HasLegacyId {
    TITLE("§6Crop Milestones", 0),
    MILESTONE_TIER("§7Pumpkin Tier 22", 1),
    NUMBER_OUT_OF_TOTAL("§e12,300§8/§e100,000", 2),
    TIME("§7In §b12m 34s", 3),
    CROPS_PER_MINUTE("§7Crops/Minute§8: §e12,345", 4),
    BLOCKS_PER_SECOND("§7Blocks/Second§8: §e19.85", 5),
    PERCENTAGE("§7Percentage: §e12.34%", 6),
    ;

    private final String str;
    private final int legacyId;

    MilestoneTextEntry(String str, int legacyId) {
        this.str = str;
        this.legacyId = legacyId;
    }

    // Constructor if new enum elements are added post-migration
    MilestoneTextEntry(String str) {
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
