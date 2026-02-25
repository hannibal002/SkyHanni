package at.hannibal2.skyhanni.features.inventory

import net.minecraft.resources.Identifier

@SuppressWarnings("unused")
object LegacyBetterContainers {

    enum class BackgroundStyle(private val displayName: String) {
        DARK_1("Dark 1"),
        DARK_2("Dark 2"),
        TRANSPARENT("Transparent"),
        LIGHT_1("Light 1"),
        LIGHT_2("Light 2"),
        LIGHT_3("Light 3"),
        ;

        override fun toString() = displayName
        private val resourceIndex = ordinal + 1

        val configId: Identifier =
            Identifier.fromNamespaceAndPath("skyhanni", "dynamic_54/style$resourceIndex/dynamic_config.json")
        val baseId: Identifier = Identifier.fromNamespaceAndPath("skyhanni", "dynamic_54/style$resourceIndex/dynamic_54.png")
        val slotId: Identifier =
            Identifier.fromNamespaceAndPath("skyhanni", "dynamic_54/style$resourceIndex/dynamic_54_slot_ctm.png")
        val buttonId: Identifier =
            Identifier.fromNamespaceAndPath("skyhanni", "dynamic_54/style$resourceIndex/dynamic_54_button_ctm.png")
    }

}
