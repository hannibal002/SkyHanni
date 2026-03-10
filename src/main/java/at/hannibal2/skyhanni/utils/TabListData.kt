package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.TablistFooterUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.mixins.hooks.tabListGuarded
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
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
object TabListData {
    private val playerOrdering = Ordering.from(TabPlayerComparator())

    @Environment(EnvType.CLIENT)
    internal class TabPlayerComparator : Comparator<PlayerInfo> {
        override fun compare(o1: PlayerInfo, o2: PlayerInfo): Int = ComparisonChain.start()
            .compareTrueFirst(o1.gameMode != GameType.SPECTATOR, o2.gameMode != GameType.SPECTATOR)
            .compare(o1.team?.name.orEmpty(), o2.team?.name.orEmpty())
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

    private suspend fun copyCommand(asComponents: Boolean = true) {
        fun Component?.localCopyFormat() = if (asComponents) this?.toString().orEmpty() else this?.formattedTextCompat().orEmpty()

        val tabHeader = header.localCopyFormat()
        val tabFooter = footer.localCopyFormat()
        val joinedResults = tablistCache.joinToString("\n") {
            val line = if (asComponents) it.toString() else it.formattedTextCompat()
            if (it.string == "") " " else line
        }
        val widgets = TabWidget.entries.filter { it.isActive }.joinToString("\n") {
            val widgetFormat = it.lines.joinToString { line ->
                if (asComponents) line.toString() else line.formattedTextCompat()
            }
            "\n${it.name} : \n$widgetFormat"
        }

        val outputString = "Header:\n\n$tabHeader\n\nBody:\n\n$joinedResults\n\nFooter:\n\n$tabFooter\n\nWidgets:$widgets"
        val copied = OSUtils.copyToClipboardAsync(outputString) ?: false
        if (!copied) return ChatUtils.chat("Failed to copy tab list data to clipboard!")

        val copyFormat = if (asComponents) "components" else "formatted text"
        ChatUtils.chat("Tab list $copyFormat copied into the clipboard!")
    }

    private fun readTabList(): List<Component>? {
        val player = MinecraftCompat.localPlayerOrNull ?: return null
        val players = playerOrdering.sortedCopy(player.connection.onlinePlayers)
        val result = tabListGuarded {
            players.map(it::getNameForDisplay)
        }
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

        val newTablistCache = readTabList()?.let { newTabList ->
            if (!SkyBlockUtils.onHypixel) DelayedRun.runDelayedReturning(2.seconds) {
                if (SkyBlockUtils.onHypixel) {
                    println("workaroundDelayedTabListUpdateAgain")
                    newTabList.also { TabListUpdateEvent(it).post() }
                } else tablistCache
            }.second() else newTabList
        }?.takeIf { it != tablistCache } ?: return
        tablistCache = newTablistCache
        TabListUpdateEvent(newTablistCache).post()

        val tabListOverlay = Minecraft.getInstance().gui.tabList
        header = tabListOverlay.header
        val newFooter = tabListOverlay.footer
        if (newFooter != footer) {
            footer = newFooter
            if (newFooter == null || newFooter.string.isEmpty()) return
            TablistFooterUpdateEvent(newFooter).post()
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcopytablistcomponent") {
            description = "Copies the tab list data to the clipboard"
            category = CommandCategory.DEVELOPER_DEBUG
            coroutineSimpleCallback { copyCommand() }
        }
        event.registerBrigadier("shcopytablist") {
            description = "Copies the tab list body to the clipboard"
            category = CommandCategory.DEVELOPER_DEBUG
            coroutineSimpleCallback { copyCommand(asComponents = false) }
        }
    }
}
