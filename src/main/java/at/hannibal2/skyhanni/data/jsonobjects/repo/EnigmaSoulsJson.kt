package at.hannibal2.hanni.data.jsonobjects.repo

import at.hannibal2.hanni.utils.LorenzVec
import com.google.gson.annotations.Expose

data class EnigmaSoulsJson(
    @Expose val areas: Map<String, List<EnigmaPosition>>,
)

data class EnigmaPosition(
    @Expose val name: String,
    @Expose val position: LorenzVec,
)
