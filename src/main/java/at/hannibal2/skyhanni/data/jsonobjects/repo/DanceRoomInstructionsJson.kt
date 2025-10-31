package at.hannibal2.hanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class DanceRoomInstructionsJson(
    @Expose val instructions: List<String>,
)
