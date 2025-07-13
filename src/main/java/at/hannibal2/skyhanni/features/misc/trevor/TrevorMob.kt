package at.hannibal2.skyhanni.features.misc.trevor

import net.minecraft.client.resources.I18n

enum class TrevorMob(val mobName: String, val renderDistance: Double) {
    COW("Cow", 68.0),
    HORSE("Horse", 90.0),
    SHEEP("Sheep", 68.0),
    PIG("Pig", 68.0),
    RABBIT("Rabbit", 43.0),
    CHICKEN("Chicken", 33.0),
    ;

    private val i18n =
        //#if MC < 1.21
        "entity.$mobName.name"
    //#else
    //$$ "entity.minecraft.${mobName.lowercase()}"
    //#endif

    val entityName get() = I18n.format(i18n)
}
