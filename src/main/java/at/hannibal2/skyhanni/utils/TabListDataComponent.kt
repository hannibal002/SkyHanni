package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.model.TabWidget
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
    private val playerOrdering = Ordering.from(TabPlayerComparator())

    @Environment(EnvType.CLIENT)
    internal class TabPlayerComparator : Comparator<PlayerInfo> {
        override fun compare(o1: PlayerInfo, o2: PlayerInfo): Int = ComparisonChain.start()
            .compareTrueFirst(o1.gameMode != GameType.SPECTATOR, o2.gameMode != GameType.SPECTATOR)
            .compare(o1.team?.name ?: "", o2.team?.name ?: "")
            .compare(o1.profile.name, o2.profile.name).result()
    }

    private var tablistCache = emptyList<Component>()
    private var dirty = false

    var header: Component? = null
        private set
    var footer: Component? = null
        private set
    var fullyLoaded = false
        internal set

    private fun copyCommand() {
        @Suppress("DEPRECATION")
        val resultList = tablistCache.map { if (it.string == "") " " else it.string }
        val tabHeader = header?.string.orEmpty()
        val tabFooter = footer?.string.orEmpty()

        val widgets = TabWidget.entries.filter { it.isActive }.joinToString("\n") {
            "\n${it.name} : \n${it.lines.joinToString("\n")}"
        }
        val joinedResults = resultList.joinToString("\n")
        val outputString = "Header:\n\n$tabHeader\n\nBody:\n\n$joinedResults\n\nFooter:\n\n$tabFooter\n\nWidgets:$widgets"
        OSUtils.copyToClipboard(outputString)
        ChatUtils.chat("Tab list components copied into the clipboard!")
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

    @HandleEvent(receiveCancelled = true)
    fun onPacketReceive(event: PacketReceivedEvent) {
        if (event.packet is ClientboundPlayerInfoUpdatePacket) dirty = true
    }

    @HandleEvent
    fun onTick() {
        if (!dirty) return
        dirty = false

        val tabList = readTabList() ?: return
        if (tablistCache != tabList) {
            tablistCache = tabList
            TabListUpdateComponentEvent(tablistCache).post()
            if (!SkyBlockUtils.onHypixel) DelayedRun.runDelayed(2.seconds) {
                if (SkyBlockUtils.onHypixel) {
                    println("workaroundDelayedTabListUpdateAgain")
                    TabListUpdateComponentEvent(tablistCache).post()
                }
            }
        }

        val tabListOverlay = Minecraft.getInstance().gui.tabList
        header = tabListOverlay.header
        footer = tabListOverlay.footer?.let {
            if (it == footer || it.string == "") footer
            else it.also { TablistFooterUpdateComponentEvent(it).post() }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcopytablistcomponent") {
            description = "Copies the tab list data to the clipboard"
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback { copyCommand() }
        }
    }
}
