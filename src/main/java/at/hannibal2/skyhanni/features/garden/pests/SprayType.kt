package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.features.garden.pests.PestApi.getPests
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

enum class SprayType(
    val displayName: String,
    val internalName: NeuInternalName? = null,
    val miscEffect: String? = null,
) {
    COMPOST("Compost"),
    PLANT_MATTER("Plant Matter"),
    DUNG("Dung"),
    HONEY_JAR("Honey Jar"),
    TASTY_CHEESE("Tasty Cheese", "CHEESE_FUEL".toInternalName()),
    FINE_FLOUR("Fine Flour", miscEffect = "§6+20☘ Farming Fortune"),
    JELLY("Jelly"),
    ;

    fun toInternalName(): NeuInternalName {
        if (internalName != null) return internalName
        return name.toInternalName()
    }

    fun getSprayEffect(): String = getPests().takeIf { it.isNotEmpty() }?.let { pests ->
        pests.joinToString("§7, §6") { it.displayName }
    } ?: this.miscEffect ?: "§cUnknown Effect"

    companion object {

        fun getByNameOrNull(name: String) = entries.firstOrNull { it.displayName == name }
        fun getByInternalName(internalName: NeuInternalName): SprayType? {
            for (spray in entries) {
                if (spray.internalName == internalName) return spray
                if (spray.name == internalName.asString()) return spray
            }
            return null
        }
        fun getByPestTypeOrAll(pestType: PestType?) = entries.filter {
            it == pestType?.spray
        }.takeIf {
            it.isNotEmpty()
        } ?: entries
    }
}
