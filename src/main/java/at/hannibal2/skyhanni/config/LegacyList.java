package at.hannibal2.skyhanni.config;

/**
 * The interface LegacyList.
 * To be used for config elements that are being migrated from ArrayLists to Enums.
 * A legacyId is not needed for new elements.
 */
public interface LegacyList {

    /**
     * Gets display string.
     *
     * @return the display string
     */
    String getStr();

    /**
     * Gets legacy id.
     *
     * @return the legacy id
     */
    int getLegacyId();

    /**
     * Legacy id or default int. This is used for legacy configs that are being migrated to enums.
     * New elements do not need a legacyId
     *
     * @return the int
     */
    default int legacyIdOrDefault() {
        return -1; // Default value
    }
}
