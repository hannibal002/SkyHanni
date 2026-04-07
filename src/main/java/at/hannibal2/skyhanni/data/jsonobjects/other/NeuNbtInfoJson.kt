package at.hannibal2.skyhanni.data.jsonobjects.other

import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.google.common.collect.ImmutableMultimap
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import java.util.UUID

data class NeuNbtInfoJson(
    @Expose @SerializedName(value = "custom_data", alternate = ["ExtraAttributes"]) val customData: JsonObject?,
    @Expose @SerializedName("SkullOwner") val profile: NeuProfileInfo?,
    @Expose val display: NeuDisplayInfo?,
    @Expose @SerializedName("ench") val enchantments: List<JsonObject>?,
    @Expose @SerializedName("Unbreakable") val unbreakable: Int?,
    @Expose @SerializedName("ItemModel") val itemModel: String?,
    @Expose @SerializedName("HideFlags") val hideFlags: Int? = null,
    @Expose @SerializedName("Explosion") val explosion: JsonObject? = null,
    @Expose @SerializedName("CustomPotionEffects") val customPotionEffects: List<JsonObject>? = null,
    @Expose val generation: Int? = null,
)

data class NeuProfileInfo(
    @Expose @SerializedName("Id") val id: String?,
    @Expose @SerializedName("Name") val name: String?,
    @Expose @SerializedName("Properties") val properties: NeuProfileProperties?,
)

data class NeuProfileProperties(
    @Expose val textures: List<NeuProfileTexture>?,
)

data class NeuProfileTexture(
    @Expose @SerializedName("Value") val value: String?,
    @Expose @SerializedName("Signature") val signature: String?,
)

data class NeuDisplayInfo(
    @Expose @SerializedName("Name") val name: String?,
    @Expose @SerializedName("Lore") val lore: List<String>?,
    @Expose val color: Int?,
)

fun NeuProfileInfo.toGameProfile(): GameProfile {
    val builder = ImmutableMultimap.builder<String, Property>()
    val texture = this.properties?.textures?.firstOrNull()
    if (texture != null) {
        builder.put("textures", Property("textures", texture.value.orEmpty(), texture.signature.orEmpty()))
    }
    return GameProfile(UUID.fromString(this.id), "hannibal2", PropertyMap(builder.build()))
}
