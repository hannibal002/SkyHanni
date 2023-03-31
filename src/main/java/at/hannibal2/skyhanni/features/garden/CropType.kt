package at.hannibal2.skyhanni.features.garden

enum class CropType(val cropName: String, val toolName: String) {
    WHEAT("Wheat", "THEORETICAL_HOE_WHEAT"),
    CARROT("Carrot", "THEORETICAL_HOE_CARROT"),
    POTATO("Potato", "THEORETICAL_HOE_POTATO"),
    NETHER_WART("Nether Wart", "THEORETICAL_HOE_WARTS"),
    PUMPKIN("Pumpkin", "PUMPKIN_DICER"),
    MELON("Melon", "MELON_DICER"),
    COCOA_BEANS("Cocoa Beans", "COCO_CHOPPER"),
    SUGAR_CANE("Sugar Cane", "THEORETICAL_HOE_CANE"),
    CACTUS("Cactus", "CACTUS_KNIFE"),
    MUSHROOM("Mushroom", "FUNGI_CUTTER"),
    ;

    override fun toString(): String {
        return cropName
    }

    companion object {
        fun getByName(name: String) = values().firstOrNull { it.cropName == name }

        // TODO find better name for this method
        fun getByNameNoNull(name: String) = getByName(name) ?: throw RuntimeException("No valid crop type '$name'")
    }
}