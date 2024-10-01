package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import kotlin.time.Duration

@SkyHanniModule
object NotificationManager {

    val notificationQueue = mutableListOf<SkyhanniNotification>()


}


data class SkyhanniNotification(val title: String, val message: String, val length: Duration, val showOverInventory: Boolean = false) {
    constructor(title: String, message: List<String>, length: Duration, showOverInventory: Boolean = false) : this(
        title,
        message.joinToString(" "),
        length,
        false,
    )
}
