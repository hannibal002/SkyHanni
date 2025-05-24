package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import com.google.gson.annotations.Expose

data class NeuAnimatedSkullsJson(
    @Expose val skins: Map<String, AnimatedSkinJson>
)

data class AnimatedSkinJson(
    @Expose val ticks: Int,
    @Expose val textures: List<String>,
)
