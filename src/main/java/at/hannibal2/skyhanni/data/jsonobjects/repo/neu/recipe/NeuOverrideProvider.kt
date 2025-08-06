package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.utils.NeuInternalName

class NeuOverrideProvider(
    val overrideItem: NeuInternalName? = null,
    val overrideCount: Int? = null,
) {
    constructor(component: NeuRecipeComponent) : this(
        overrideItem = component.internalName,
        overrideCount = component.count,
    )
}
