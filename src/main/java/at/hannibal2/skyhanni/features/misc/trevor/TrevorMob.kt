package at.hannibal2.skyhanni.features.misc.trevor

import net.minecraft.client.resources.language.I18n

enum class TrevorMob(val mobName: String, val renderDistance: Double) {
    COW("Cow", 68.0),
    HORSE("Horse", 90.0),
    SHEEP("Sheep", 68.0),
    PIG("Pig", 68.0),
    RABBIT("Rabbit", 43.0),
    CHICKEN("Chicken", 33.0),
    ;

    private val i18n = "entity.minecraft.${mobName.lowercase()}"

    val entityName: String get() = I18n.get(i18n)

    companion object {
        fun findByName(name: String) = entries.find { it.mobName.contains(name) || it.entityName.contains(name) }
    }
}
