package at.hannibal2.skyhanni.config.features.pets.display

import at.hannibal2.skyhanni.config.storage.Resettable

interface ResettableScalableConfig : Resettable {
    val scalar: Float get() = 1.0f
}
