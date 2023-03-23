package at.hannibal2.skyhanni.features.garden

enum class CropType(val cropName: String, val toolName: String) {
    WHEAT("Wheat", "THEORETICAL_HOE_WHEAT"),
    CARROT("Carrot", "THEORETICAL_HOE_CARROT"),
    POTATO("Potato", "THEORETICAL_HOE_POTATO"),
    PUMPKIN("Pumpkin", "PUMPKIN_DICER"),
    SUGAR_CANE("Sugar Cane", "THEORETICAL_HOE_CANE"),
    MELON("Melon", "MELON_DICER"),
    CACTUS("Cactus", "CACTUS_KNIFE"),
    COCOA_BEANS("Cocoa Beans", "COCO_CHOPPER"),
    MUSHROOM("Mushroom", "FUNGI_CUTTER"),
    NETHER_WART("Nether Wart", "THEORETICAL_HOE_WARTS"),
    ;

    companion object {
        fun getByName(name: String) = values().firstOrNull { it.cropName == name }

        // TODO find better name for this method
        fun getByNameNoNull(name: String) = getByName(name) ?: throw RuntimeException("No valid crop type '$name'")
    }
}