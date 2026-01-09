package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import java.io.ByteArrayInputStream
import java.util.Base64

data class NeuItemJson(
    @Expose @SerializedName("itemid") val itemId: String,
    @Expose @SerializedName("displayname") val displayName: String,
    @Expose @SerializedName("nbttag") val nbtTagString: String,
    @Expose val damage: Int,
    @Expose val lore: List<String>,
    @Expose @SerializedName("internalname") val internalName: NeuInternalName,
    @Expose @SerializedName("crafttext") val craftText: String,
    @Expose @SerializedName("clickcommand") val clickCommand: String,
    @Expose @SerializedName("modver") val modVersion: String,
    @Expose val infoType: String,
    @Expose val info: List<String>,
) {
    private fun getParsedNBT(): CompoundTag {
        return try {
            val decodedBytes = Base64.getDecoder().decode(nbtTagString.toByteArray(Charsets.UTF_8))
            val inputStream = ByteArrayInputStream(decodedBytes)
            NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap())
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse NBT tag: $nbtTagString", e)
        }
    }

    val nbtTag get() = getParsedNBT()
}
