package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HoneyhiveReminderConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Reminder when the cooldown for looting Honeyhives is over.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Queen Bee Notification", desc = "Show a title when Queen Bee shard procs on a Honeyhive instantly refilling it.")
    @ConfigEditorBoolean
    var queenBeeNotification: Boolean = true

    @Expose
    @ConfigOption(
        name = "Remind Outside Torrhus Canyon",
        desc = "Remind when collecting Honeyhives is available even when you are outside the Torrhus Canyon.",
    )
    @ConfigEditorBoolean
    var reminderOutsideTorrhus: Boolean = false

}
