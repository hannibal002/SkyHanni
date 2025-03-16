package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class EmojiJson(@Expose @SerializedName("emoji_names") val emojiNames: Map<String, Int>)
