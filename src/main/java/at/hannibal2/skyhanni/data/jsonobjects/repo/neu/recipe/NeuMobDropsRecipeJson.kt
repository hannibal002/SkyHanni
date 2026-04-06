package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.utils.BasePrimitiveRecipe
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class NeuMobDropsRecipeJson(
    @Expose override val type: NeuRecipeType = NeuRecipeType.MOB_DROP,
    @Expose val name: String,
    @Expose val render: String? = null,
    @Expose val panorama: String? = null,
    @Expose val level: Int,
    @Expose val coins: Int,
    @Expose val xp: Int? = null,
    @Expose @SerializedName("combat_xp") val combatXp: Int? = null,
    @Expose val drops: List<NeuDropJson>,
    @Expose val extra: List<String> = emptyList(),
) : NeuAbstractRecipe {
    // These cannot be by lazy since this class cannot be KSerializable.
    override val primitiveIngredients get() = listOf(PrimitiveIngredient(monsterByName(name)))
    private val primitiveOutputs get() = drops.map { it.id.toPrimitiveIngredient() }
    override fun getPrimitiveRecipe(itemJson: NeuItemJson): BasePrimitiveRecipe = BasePrimitiveRecipe(
        listOf(PrimitiveIngredient(itemJson.internalName)),
        primitiveOutputs,
        this.type,
    )

    companion object {
        private fun monsterByName(name: String) = NeuInternalName.fromItemNameOrInternalName(name)
    }
}

data class NeuDropJson(
    @Expose val id: NeuRecipeComponent,
    /**
     * Because of variability in formatting, this is a String.
     * Can be of the format(s);
     * "100%"
     * "0.00004%"
     * "x15-27"
     */
    @Expose val chance: String,
    @Expose val extra: List<String> = emptyList(),
)
