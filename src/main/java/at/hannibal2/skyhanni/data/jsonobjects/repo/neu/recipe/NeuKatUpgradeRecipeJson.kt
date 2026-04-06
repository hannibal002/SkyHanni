package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.utils.DurationPrimitiveRecipe
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.SKYBLOCK_COIN
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
) : NeuAbstractRecipe {
    val duration by lazy { time.seconds }
    override val primitiveIngredients by lazy {
        buildList {
            items.mapNotNull { it.toPrimitiveIngredientOrEmpty() }.forEach { add(it) }
            add(PrimitiveIngredient(input))
            add(PrimitiveIngredient(SKYBLOCK_COIN, coins))
        }
    }
    private val primitiveOutput by lazy { PrimitiveIngredient(output) }
    override fun getPrimitiveOutputs(itemJson: NeuItemJson) = listOf(primitiveOutput)
    override fun getPrimitiveRecipe(itemJson: NeuItemJson): DurationPrimitiveRecipe =
        super.getBasePrimitiveRecipe(itemJson).withDuration(duration)
}
