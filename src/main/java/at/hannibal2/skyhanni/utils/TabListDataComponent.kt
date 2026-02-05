package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.TabListUpdateComponentEvent
import at.hannibal2.skyhanni.events.TablistFooterUpdateComponentEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.mixins.hooks.tabListGuard
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import com.google.common.collect.ComparisonChain
import com.google.common.collect.Ordering
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.world.level.GameType
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TabListDataComponent {
    private var tablistCache = emptyList<Component>()
    //private var debugCache: List<String>? = null

    private var header: Component? = null
    private var footer: Component? = null

    var fullyLoaded = false

    // TODO replace with TabListUpdateEvent
    @Deprecated("replace with TabListUpdateEvent")
    fun getTabList() = /*debugCache ?: */ tablistCache
    fun getHeader() = header
    fun getFooter() = footer

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Tab List Debug Cache")
        /*debugCache?.let {
            event.addData {
                add("debug active!")
                add("lines: (${it.size})")
                for (line in it) {
                    add(" '$line'")
                }
            }
        } ?: */event.addIrrelevant("not active.")
    }

    private fun toggleDebug() {
        /*if (debugCache != null) {
            ChatUtils.chat("Disabled tab list debug.")
            debugCache = null
            return
        }
        SkyHanniMod.launchCoroutine("tab list toggle debug") {
            val clipboard = OSUtils.readFromClipboard() ?: return@launchCoroutine
            debugCache = clipboard.lines()
            ChatUtils.chat("Enabled tab list debug with your clipboard.")
        }*/
    }

    private fun copyCommand() {
        val resultList = mutableListOf<String>()
        for (line in getTabList()) {
            val tabListLine = line
            if (tabListLine.string != "") resultList.add("'$tabListLine'")
        }

        val tabHeader = header?.string.orEmpty()
        val tabFooter = footer?.string.orEmpty()

        val widgets = TabWidget.entries.filter { it.isActive }
            .joinToString("\n") { "\n${it.name} : \n${it.lines.joinToString("\n")}" }
        val string =
            "Header:\n\n$tabHeader\n\nBody:\n\n${resultList.joinToString("\n")}\n\nFooter:\n\n$tabFooter\n\nWidgets:$widgets"

        OSUtils.copyToClipboard(string)
        ChatUtils.chat("Tab list copied into the clipboard!")
    }

    private val playerOrdering = Ordering.from(PlayerComparator())

    @Environment(EnvType.CLIENT)
    internal class PlayerComparator : Comparator<PlayerInfo> {

        override fun compare(o1: PlayerInfo, o2: PlayerInfo): Int {
            val team1 = o1.team
            val team2 = o2.team
            return ComparisonChain.start().compareTrueFirst(o1.gameMode != GameType.SPECTATOR, o2.gameMode != GameType.SPECTATOR)
                .compare(
                    if (team1 != null) team1.name else "",
                    if (team2 != null) team2.name else "",
                )
                .compare(o1.profile.name, o2.profile.name).result()
        }
    }

    private fun readTabList(): List<Component>? {
        val player = MinecraftCompat.localPlayerOrNull ?: return null
        val players = playerOrdering.sortedCopy(player.connection.onlinePlayers)
        val result = mutableListOf<Component>()
        tabListGuard = true
        for (info in players) {
            val name = Minecraft.getInstance().gui.tabList.getNameForDisplay(info)
            result.add(name)
        }
        tabListGuard = false
        return if (result.size < 80) result.dropLast(1)
        else result.subList(0, 80)
    }

    var dirty = false

    @HandleEvent(receiveCancelled = true)
    fun onPacketReceive(event: PacketReceivedEvent) {
        if (event.packet is ClientboundPlayerInfoUpdatePacket) {
            dirty = true
        }
    }

    @HandleEvent
    fun onTick() {
        if (!dirty) return
        dirty = false

        val tabList = readTabList() ?: return
        if (tablistCache != tabList) {
            tablistCache = tabList
            TabListUpdateComponentEvent(tablistCache).post()
            if (!SkyBlockUtils.onHypixel) {
                workaroundDelayedTabListUpdateAgain()
            }
        }

        val tabListOverlay = Minecraft.getInstance().gui.tabList
        header = tabListOverlay.header

        val tabFooter = tabListOverlay.footer
        if (tabFooter != footer && tabFooter != null && tabFooter.string != "") {
            TablistFooterUpdateComponentEvent(tabFooter).post()
        }
        footer = tabFooter
    }

    private fun workaroundDelayedTabListUpdateAgain() {
        DelayedRun.runDelayed(2.seconds) {
            if (SkyBlockUtils.onHypixel) {
                println("workaroundDelayedTabListUpdateAgain")
                TabListUpdateComponentEvent(tablistCache).post()
            }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtesttablistcomponent") {
            description = "Set your clipboard as a fake tab list."
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback { toggleDebug() }
        }
        event.registerBrigadier("shcopytablistcomponent") {
            description = "Copies the tab list data to the clipboard"
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback { copyCommand() }
        }
    }
}
