package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

abstract class NeuAbstractRecipe {
    abstract val type: NeuRecipeType

    abstract fun getPrimitiveInputs(itemJson: NeuItemJson): List<PrimitiveIngredient>

    open fun getPrimitiveOutputs(itemJson: NeuItemJson): List<PrimitiveIngredient> = listOf(
        getPrimitiveOutput(itemJson)
    )

    protected open val outputOverride: NeuOverrideProvider? = null

    private fun getPrimitiveOutput(itemJson: NeuItemJson): PrimitiveIngredient {
        val craftAmount = outputOverride?.overrideCount ?: 1
        val outputInternalName = outputOverride?.overrideItem ?: itemJson.internalName
        return PrimitiveIngredient(outputInternalName, craftAmount)
    }

    companion object {
        class AbstractNeuRecipeDeserializer : JsonDeserializer<NeuAbstractRecipe> {
            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext,
            ): NeuAbstractRecipe {
                val obj = json.asJsonObject
                val typeId = obj.get("type").asString
                val recipeType = NeuRecipeType.fromNeuIdOrNull(typeId)
                    ?: throw IllegalArgumentException("Unknown recipe type: $typeId")
                return ConfigManager.gson.fromJson(obj, recipeType.castClazz)
            }
        }
    }
}
