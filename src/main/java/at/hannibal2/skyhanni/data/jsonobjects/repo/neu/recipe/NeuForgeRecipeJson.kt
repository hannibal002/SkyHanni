package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration.Companion.seconds

@KSerializable
data class NeuForgeRecipeJson(
    @Expose override val type: NeuRecipeType = NeuRecipeType.FORGE,
    @Expose val inputs: List<NeuRecipeComponent>,
    @Expose val count: Int,
    @Expose val overrideOutputId: NeuInternalName? = null,
    @Expose @SerializedName("duration") private val durationSeconds: Int,
) : NeuAbstractRecipe() {
    val duration by lazy { durationSeconds.seconds }

    override fun getPrimitiveInputs(itemJson: NeuItemJson): List<PrimitiveIngredient> =
        inputs.mapNotNull { it.toPrimitiveIngredientOrNull() }

    override val outputOverride: NeuOverrideProvider = NeuOverrideProvider(
        overrideItem = overrideOutputId,
        overrideCount = count,
    )
}
