package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
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
        class AbstractNeuRecipeSerializer : JsonSerializer<NeuAbstractRecipe> {
            override fun serialize(
                src: NeuAbstractRecipe,
                typeOfSrc: Type,
                context: JsonSerializationContext,
            ): JsonElement = context.serialize(src, src.type.castClazz)
        }

        class AbstractNeuRecipeDeserializer : JsonDeserializer<NeuAbstractRecipe> {
            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext,
            ): NeuAbstractRecipe {
                val obj = json.asJsonObject
                val typeId = obj.get("type").asString
                val recipeType = NeuRecipeType.fromNeuId(typeId)
                return ConfigManager.gson.fromJson(obj, recipeType.castClazz)
            }

        }
    }
}
