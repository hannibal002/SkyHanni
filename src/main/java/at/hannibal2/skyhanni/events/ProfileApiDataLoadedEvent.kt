package at.hannibal2.skyhanni.events

import com.google.gson.JsonObject

class ProfileApiDataLoadedEvent(val profileData: JsonObject) : LorenzEvent()