package at.hannibal2.hanni.features.rift.area.wyldwoods

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.LocationUtils.distanceToPlayer
import kotlin.time.Duration.Companion.milliseconds

@HanniModule
object ShyCruxWarnings {

    private val config get() = RiftApi.config.area.wyldWoods
    private val shyNames = arrayOf("I'm ugly! :(", "Eek!", "Don't look at me!", "Look away!")

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onTick() {
        if (!config.shyWarning) return
        checkForShy()
    }

    private fun checkForShy() {
        if (EntityUtils.getAllEntities().any { it.name in shyNames && it.distanceToPlayer() < 8 }) {
            TitleManager.sendTitle("§eLook away!", duration = 150.milliseconds)
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(9, "rift.area.wyldWoodsConfig", "rift.area.wyldWoods")
    }
}
