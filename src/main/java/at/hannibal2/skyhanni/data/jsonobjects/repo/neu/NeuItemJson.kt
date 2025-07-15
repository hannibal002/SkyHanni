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
    @Expose @SerializedName("nbttag") val nbtTagString: String,
    @Expose val damage: Int? = null,
    @Expose val lore: List<String> = emptyList(),
    @Expose @SerializedName("internalname") val internalName: NeuInternalName,
    @Expose @SerializedName("crafttext") val craftText: String,
    @Expose @SerializedName("useneucraft") val useNeuCraft: Boolean = false,
    @Expose @SerializedName("clickcommand") val clickCommand: String,
    @Expose @SerializedName("modver") val modVersion: String,
    @Expose val vanilla: Boolean = false,
    @Expose val infoType: String,
    @Expose val info: List<String>,
    @Expose val recipe: NeuCraftingRecipeJson? = null,
    @Expose val recipes: List<NeuAbstractRecipe> = emptyList(),
    @Expose val count: Int? = null,
) {
    //#if MC < 1.21
    private fun getParsedNBT(): NBTTagCompound {
        return try {
            val decodedBytes = Base64.getDecoder().decode(nbtTagString.toByteArray(Charsets.UTF_8))
            val inputStream = ByteArrayInputStream(decodedBytes)
            CompressedStreamTools.readCompressed(inputStream)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse NBT tag: $nbtTagString", e)
        }
    }
    //#else
    //$$ private fun getParsedNBT(): NbtCompound {
    //$$     return try {
    //$$         val decodedBytes = Base64.getDecoder().decode(nbtTagString.toByteArray(Charsets.UTF_8))
    //$$         val inputStream = ByteArrayInputStream(decodedBytes)
    //$$         NbtIo.readCompressed(inputStream, NbtSizeTracker.ofUnlimitedBytes())
    //$$     } catch (e: Exception) {
    //$$         throw IllegalArgumentException("Failed to parse NBT tag: $nbtTagString", e)
    //$$     }
    //$$ }
    //
    //#endif

    val nbtTag by lazy { getParsedNBT() }

    private val nbtListRegex = Regex("([\\[,])\\d+:")
    private val fixedNbt by lazy { nbtTagString.replace(nbtListRegex, "$1") }

    private fun convertToNeuNbt(): NeuNbtInfoJson? = runCatching {
        ConfigManager.gson.fromJson<NeuNbtInfoJson>(fixedNbt)
    }.getOrElse {
        ErrorManager.logErrorWithData(
            throwable = it,
            "Error converting NBT to NeuNbtInfoJson",
            extraData = listOf(
                "nbtTagString" to nbtTagString,
                "itemId" to itemId,
                "internalName" to internalName,
                "fixedNbt" to  fixedNbt
            ).toTypedArray()
        )
        null
    }

    val neuNbt by lazy { convertToNeuNbt() }
}
