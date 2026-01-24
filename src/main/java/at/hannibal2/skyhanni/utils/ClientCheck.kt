package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.bold
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.underlined
import at.hannibal2.skyhanni.utils.compat.url
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import kotlinx.coroutines.Job
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ClientCheck {
    private val clientName: String? by lazy {
        when {
            PlatformUtils.isModInstalled("ichor") ->
                "Lunar Client"
            PlatformUtils.isAnyModInstalled("feather", "feather-loader") ->
                "Feather Client"
            else ->
                null
        }
    }

    @HandleEvent
    fun onProfileJoin() {
        val clientName = clientName ?: return

        ChatUtils.chat(
            componentBuilder {
                append("You appear to be using ")
                append(clientName) { bold = true }
                append(
                    ". This is a closed source client that may cause issues we are unable to fix. " +
                    "We strongly recommend switching to a supported configuration, such as "
                )
                append("Modrinth Launcher") {
                    url = "https://modrinth.com/app"
                    underlined = true
                }
                append(" or ")
                append("Prism Launcher") {
                    url = "https://prismlauncher.org/"
                    underlined = true
                }
                append(".")
            }
        )
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Client Check")
        event.addData("Client name: $clientName")
    }
}
