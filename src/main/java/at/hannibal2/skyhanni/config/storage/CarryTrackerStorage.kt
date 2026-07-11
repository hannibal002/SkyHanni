package at.hannibal2.skyhanni.config.storage

import com.google.gson.annotations.Expose

class CarryTrackerStorage {
    @Expose
    val carryPrices: MutableMap<String, Double> = mutableMapOf()
}
