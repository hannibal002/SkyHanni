package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.NoConfigLink
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MouseLockConfig {

    @ConfigOption(
        name = "Note",
        desc = "You can use the §e/shmouselock §rcommand to lock and unlock your mouse rotation.",
    )
    @ConfigEditorInfoText
    var notice: String = ""

    @Expose
    @ConfigOption(name = "Chat Message", desc = "Show a message in chat when toggling mouse lock.")
    @ConfigEditorBoolean
    var chatMessage: Boolean = true

    @Expose
    @ConfigOption(
        name = "Unlock on Teleport",
        desc = "Choose whether teleporting to a plot should unlock your mouse rotation.",
    )
    @ConfigEditorDropdown
    var unlockOnTeleport: UnlockOnTeleport = UnlockOnTeleport.ALWAYS

    @Expose
    @NoConfigLink
    val display: Position = Position(400, 200, 0.8f)

    enum class UnlockOnTeleport(private val displayName: String, val condition: (String) -> Boolean) {
        ALWAYS("Always", { true }),
        BARN_ONLY("Barn Only", { it == "The Barn" }),
        NEVER("Never", { false }),
        ;

        override fun toString() = displayName
    }
}
