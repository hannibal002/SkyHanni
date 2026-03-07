package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.utils.BasePrimitiveRecipe
import at.hannibal2.skyhanni.utils.DurationPrimitiveRecipe
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.NeuInternalName
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
) : NeuAbstractRecipe<DurationPrimitiveRecipe>() {
    val duration by lazy { durationSeconds.seconds }
    override fun getPrimitiveRecipe(itemJson: NeuItemJson): DurationPrimitiveRecipe = BasePrimitiveRecipe(
        inputs.mapNotNull { it.toPrimitiveIngredientOrNull() }.toSet(),
        getPrimitiveOutputs(itemJson, count),
        this.type,
    ).withDuration(duration)
}
