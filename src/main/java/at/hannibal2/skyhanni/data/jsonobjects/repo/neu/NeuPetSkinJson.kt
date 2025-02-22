package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import at.hannibal2.skyhanni.api.CurrentPetApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import java.io.ByteArrayInputStream
import java.util.Base64

data class NeuPetSkinJson(
    @Expose @SerializedName("itemid") val itemId: String,
    @Expose @SerializedName("displayname") val displayName: String,
    @Expose @SerializedName("nbttag") val nbtTagString: String,
    @Expose val damage: Int,
    @Expose val lore: List<String>,
    @Expose @SerializedName("internalname") val internalNameStr: String,
    @Expose @SerializedName("crafttext") val craftText: String,
    @Expose @SerializedName("clickcommand") val clickCommand: String,
    @Expose @SerializedName("modver") val modVersion: String,
    @Expose val infoType: String,
    @Expose val info: List<String>
) {
    /**
     * Parses the NBT tag from the JSON into an NBTTagCompound.
     * @return Parsed NBTTagCompound object.
     * @throws IllegalArgumentException if the NBT parsing fails.
     */
    private val nbtTag: NBTTagCompound? get() = try {
        val decodedBytes = Base64.getDecoder().decode(nbtTagString.toByteArray(Charsets.UTF_8))
        val inputStream = ByteArrayInputStream(decodedBytes)
        CompressedStreamTools.readCompressed(inputStream)
    } catch (e: Exception) {
        throw IllegalArgumentException("Failed to parse NBT tag: $nbtTagString", e)
    }

    @Suppress("SpreadOperator")
    val itemStack by lazy {
        nbtTag?.let {
            ItemUtils.createSkull(
                displayName,
                it.getString("ID"),
                it.getSkullTexture(),
                *lore.toTypedArray()
            )
        }
    }
    val rarity: LorenzRarity? = rarityPattern.firstMatcher(lore) { LorenzRarity.getByName(group("rarity")) }
    val internalName = internalNameStr.toInternalName()

    @SkyHanniModule
    companion object {
        /**
         * REGEX-TEST: §9§lRARE COSMETIC
         * REGEX-TEST: §d§lMYTHIC COSMETIC
         */
        private val rarityPattern by CurrentPetApi.patternGroup.pattern(
            "skin.rarity",
            "(?:§.)+(?<rarity>[A-Z]+) COSMETIC",
        )
    }
}
