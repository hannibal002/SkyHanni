package at.hannibal2.skyhanni.data.jsonobjects.other

import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import java.util.UUID

data class NeuNbtInfoJson(
    @Expose @SerializedName("HideFlags") val hideFlags: Int?,
    @Expose @SerializedName("Unbreakable") val unbreakable: NbtBoolean?,
    @Expose @SerializedName("SkullOwner") val skullOwner: SkullOwnerInfo?,
    @Expose @SerializedName("display") val display: DisplayInfo?,
    @Expose @SerializedName("ExtraAttributes") val extraAttributes: JsonObject?,
    @Expose @SerializedName("Explosion") val explosion: JsonObject?,
    @Expose @SerializedName("CustomPotionEffects") val customPotionEffects: List<JsonObject>?,
    @Expose @SerializedName("ench") val enchantments: List<JsonObject>?,
    @Expose @SerializedName("ItemModel") val itemModel: String?,
    @Expose val overrideMeta: NbtBoolean?,
    @Expose val generation: Int?,
    @Expose val resolved: NbtBoolean?,
)

data class SkullOwnerInfo(
    @Expose @SerializedName("Id") val uuid: String?,
    @Expose @SerializedName("Properties") val properties: PropertiesInfo?,
    @Expose val hypixelPopulated: NbtBoolean?,
    @Expose @SerializedName("Name") val name: String?,
)

data class PropertiesInfo(
    @Expose @SerializedName("textures") val textures: List<TextureInfo>?,
)

data class TextureInfo(
    @Expose @SerializedName("Value") val value: String?,
    @Expose @SerializedName("Signature") val signature: String?,
)

data class DisplayInfo(
    @Expose @SerializedName("Name") val name: String?,
    @Expose @SerializedName("Lore") val lore: List<String>?,
    @Expose val color: Int?,
)

fun SkullOwnerInfo.toGameProfile(): GameProfile {
    val profile = GameProfile(UUID.fromString(this.uuid), "hannibal2")
    val textures = this.properties?.textures?.get(0)
    profile.properties.put("textures", Property("textures", textures?.value.orEmpty(), textures?.signature.orEmpty()))
    return profile
}

data class NbtBoolean(val boolean: Boolean) {
    fun asString(): String {
        return if (boolean) "1b" else "0b"
    }

    companion object {
        fun fromString(value: String): NbtBoolean {
            return if (value == "1b") {
                NbtBoolean(true)
            } else {
                NbtBoolean(false)
            }
        }
    }
}
