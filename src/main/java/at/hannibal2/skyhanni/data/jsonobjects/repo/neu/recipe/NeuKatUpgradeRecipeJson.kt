package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.seconds

@KSerializable
data class NeuKatUpgradeRecipeJson(
    @Expose override val type: NeuRecipeType = NeuRecipeType.KAT_UPGRADE,
    @Expose val coins: Double,
    @Expose private val time: Int,
    @Expose val input: NeuInternalName,
    @Expose val output: NeuInternalName,
    @Expose val items: List<NeuRecipeComponent> = emptyList(),
) : NeuAbstractRecipe() {
    val duration by lazy { time.seconds }

    override fun getPrimitiveInputs(itemJson: NeuItemJson) = buildList {
        items.forEach { add(it.toPrimitiveIngredient()) }
        add(PrimitiveIngredient(input))
        add(PrimitiveIngredient.coinIngredient(coins))
    }

    override val outputOverride: NeuOverrideProvider = NeuOverrideProvider(output)
}
