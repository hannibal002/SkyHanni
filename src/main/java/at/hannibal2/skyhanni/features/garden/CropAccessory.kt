package at.hannibal2.skyhanni.features.garden

enum class CropAccessory(val internalName: String, private val affectedCrops: Set<CropType>, private val fortune: Double) {
    CROPIE("CROPIE_TALISMAN", setOf(CropType.WHEAT, CropType.POTATO, CropType.CARROT), 10.0),
    SQUASH(
        "SQUASH_RING",
        setOf(CropType.WHEAT, CropType.POTATO, CropType.CARROT, CropType.COCOA_BEANS, CropType.MELON, CropType.PUMPKIN),
        20.0
    ),
    FERMENTO("FERMENTO_ARTIFACT", CropType.values().toSet(), 30.0);

    fun getFortune(cropType: CropType): Double {
        return if (this.affectedCrops.contains(cropType)) this.fortune else 0.0
    }

    companion object {
        fun getByName(internalName: String) = values().firstOrNull { it.internalName == internalName }
    }
}