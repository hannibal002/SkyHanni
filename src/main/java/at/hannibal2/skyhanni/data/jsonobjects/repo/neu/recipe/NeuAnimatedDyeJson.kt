package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose

data class NeuAnimatedDyeJson(
    @Expose val animated: Map<NeuInternalName, List<String>>,
    @Expose val static: Map<NeuInternalName, String>,
    @Expose val vanilla: Map<NeuInternalName, String>,
)
