package at.hannibal2.hanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class PlayerChatFilterJson(
    @Expose val filters: List<MultiFilterJson>,
)
