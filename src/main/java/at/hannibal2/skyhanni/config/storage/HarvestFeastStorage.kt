package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteFeastData
import com.google.gson.annotations.Expose

class HarvestFeastStorage {
    @Expose
    var harvestFeastSendingAsked: Boolean = false

    @Expose
    var storedHarvestFeastData: EliteFeastData? = null

    @Expose
    var lastHarvestFeastSubmitYear: Int = -1

    @Expose
    var lastHarvestFeastSubmitMonth: Int = -1
}
