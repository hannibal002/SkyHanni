package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.other.NeuNbtInfoJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuAbstractRecipe
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuCraftingRecipeJson
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
//#if MC < 1.21
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
//#else
//$$ import com.google.gson.JsonObject
//$$ import net.minecraft.nbt.NbtCompound
//$$ import net.minecraft.nbt.NbtIo
//$$ import net.minecraft.nbt.NbtSizeTracker
//#endif
import java.io.ByteArrayInputStream
import java.util.Base64

@KSerializable
data class NeuItemJson(
    @Expose @SerializedName("itemid") var itemId: String,
    @Expose @SerializedName("displayname") val displayName: String? = null,
    //#if MC < 1.21
    @Expose @SerializedName("nbttag") private val nbtTagString: String,
    //#else
    //$$ @Expose @SerializedName("nbttag") private val nbtTagAny: Any,
    //#endif
    @Expose val damage: Int? = null,
    @Expose val lore: List<String> = emptyList(),
    @Expose @SerializedName("internalname") val internalName: NeuInternalName,
    @Expose @SerializedName("crafttext") val craftText: String? = null,
    @Expose @SerializedName("useneucraft") val useNeuCraft: Boolean = false,
    @Expose @SerializedName("clickcommand") val clickCommand: String? = null,
    @Expose @SerializedName("modver") val modVersion: String? = null,
    @Expose val vanilla: Boolean = false,
    @Expose val infoType: String? = null,
    @Expose val info: List<String> = emptyList(),
    @Expose val recipe: NeuCraftingRecipeJson? = null,
    @Expose val recipes: List<NeuAbstractRecipe> = emptyList(),
    @Expose val count: Int? = null,
) {
    companion object {
        private val nbtListRegex = Regex("([\\[,])\\d+:")
    }

    private val fixedNbtTagString by lazy {
        //#if MC < 1.21
        nbtTagString
        //#else
        //$$ when (nbtTagAny) {
        //$$    is String -> nbtTagAny
        //$$    is JsonObject -> nbtTagAny["nbttag"]?.asString.orEmpty()
        //$$    else -> throw IllegalArgumentException("nbtTagAny must be a String or JsonObject")
        //$$ }
        //#endif
    }
    val nbtTag by lazy { getParsedNBT() }

    private val neuParsableNbt by lazy { fixedNbtTagString.replace(nbtListRegex, "$1") }
    val neuNbt by lazy { convertToNeuNbt() }

    //#if MC < 1.21
    private fun getParsedNBT(): NBTTagCompound {
        //#else
        //$$ private fun getParsedNBT(): NbtCompound {
        //#endif
        return try {
            val decodedBytes = Base64.getDecoder().decode(fixedNbtTagString.toByteArray(Charsets.UTF_8))
            val inputStream = ByteArrayInputStream(decodedBytes)
            //#if MC < 1.21
            CompressedStreamTools.readCompressed(inputStream)
            //#else
            //$$ NbtIo.readCompressed(inputStream, NbtSizeTracker.ofUnlimitedBytes())
            //#endif
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse NBT tag: $fixedNbtTagString", e)
        }
    }

    private fun convertToNeuNbt(): NeuNbtInfoJson? = runCatching {
        ConfigManager.gson.fromJson<NeuNbtInfoJson>(neuParsableNbt)
    }.getOrElse {
        ErrorManager.logErrorWithData(
            throwable = it,
            "Error converting NBT to NeuNbtInfoJson",
            extraData = listOf(
                "fixedNbtTagString" to fixedNbtTagString,
                "itemId" to itemId,
                "internalName" to internalName,
                "neuParsableNbt" to neuParsableNbt
            ).toTypedArray()
        )
        null
    }
}
