package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NotificationUtil

object TestNotificationCommand {
    fun command() {
        LorenzUtils.chat("Sending Noti")
        NotificationUtil.create(
            "SkyHanni Notification Test",
            "If you can see that, congrats!",
            "https://preview.redd.it/vpvvvmssblt31.png?auto=webp&s=aef78d39dd0ab0f71ccf4fc61ee5ae4f21008a34"
        )
    }

}
