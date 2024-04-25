package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

class PlayerChatFilterJson(
    @Expose val filters: List<MultiFilterJson>
)
