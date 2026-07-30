package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.PlayerSuggestions
import at.hannibal2.skyhanni.config.enums.OutsideSBFeature
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.player.RemotePlayer

@SkyHanniModule
object MarkedPlayerManager {

    val config get() = SkyHanniMod.feature.gui.markedPlayers

    private val playerNamesToMark = mutableListOf<String>()
    private val markedPlayers = mutableMapOf<String, RemotePlayer>()

    private val patternGroup = RepoPattern.group("misc.markedplayer")

    /**
     * REGEX-TEST: [400] HiZe_ ▒
     * REGEX-TEST: [318] wings_wacr ᛝ
     * REGEX-TEST: [321] botbob21 ᛝ
     * REGEX-TEST: [42] VoidW_
     * REGEX-TEST: [151] Phoenix_325
     */
    private val tabPlayerName by patternGroup.pattern(
        "tabplayername-no-color",
        "\\[(?<level>.*)] (?<name>[A-z0-9_]+)(?<symbol>.*)?",
    )

    private val notifyList = mutableSetOf<String>()
    private val currentLobbyPlayers = mutableSetOf<String>()
    private var personOfInterest = listOf<String>()

    fun isMarkedPlayer(name: String): Boolean =
        playerNamesToMark.any { it.equals(name, ignoreCase = true) }

    private fun getStoredName(name: String): String? =
        playerNamesToMark.firstOrNull { it.equals(name, ignoreCase = true) }

    @HandleEvent
    private fun onEntityEnterWorld(event: EntityEnterWorldEvent<RemotePlayer>) {
        if (!isEnabled()) return
        val entity = event.entity
        val name = entity.cleanName
        if (isMarkedPlayer(name)) {
            markedPlayers[name] = entity
            entity.setColor()
        }
    }

    // only gets called on command or on config change, so performance impact is minimal
    @OptIn(AllEntitiesGetter::class)
    private fun findPlayers() {
        for (entity in EntityUtils.getPlayerEntities()) {
            if (entity in markedPlayers.values) continue

            val name = entity.cleanName
            if (isMarkedPlayer(name)) {
                markedPlayers[name] = entity
                entity.setColor()
            }
        }
    }

    private fun refreshColors() =
        markedPlayers.forEach {
            it.value.setColor()
        }

    private fun RemotePlayer.setColor() {
        RenderLivingEntityHelper.setEntityColor(
            this,
            config.entityColor.get().toColor().addAlpha(127),
            ::isEnabled,
        )
    }

    @Suppress("DEPRECATION")
    private fun isEnabled() = (SkyBlockUtils.inSkyBlock || OutsideSBFeature.MARKED_PLAYERS.isSelected()) &&
        config.highlightInWorld.get()

    fun replaceInChat(string: String): String {
        if (!config.highlightInChat) return string

        val color = config.chatColor.getChatColor()

        val markedPlayer = playerNamesToMark.firstOrNull { player ->
            string.contains(player, ignoreCase = true)
        } ?: return string

        return string.replaceFirst(
            markedPlayer,
            "$color$markedPlayer§r",
            ignoreCase = true,
        )
    }

    @HandleEvent
    private fun onConfigLoad() {
        config.markOwnName.whenChanged { _, new ->
            val name = PlayerUtils.getName()
            if (new) {
                if (!isMarkedPlayer(name)) {
                    playerNamesToMark.add(name)
                }
            } else {
                getStoredName(name)?.let {
                    playerNamesToMark.remove(it)
                }
            }
        }
        config.entityColor.onToggle(::refreshColors)
        config.joinLeaveMessage.playersList.onToggle {
            personOfInterest = config.joinLeaveMessage.playersList.get().split(",").map { it.trim() }
        }
        config.highlightInWorld.onToggle(::findPlayers)
    }

    @HandleEvent
    private fun onWorldChange() {
        if (!MinecraftCompat.localPlayerExists) return

        markedPlayers.clear()
        notifyList.clear()
        currentLobbyPlayers.clear()
        if (config.markOwnName.get()) {
            val name = PlayerUtils.getName()
            if (!isMarkedPlayer(name)) {
                playerNamesToMark.add(name)
            }
        }
    }

    @HandleEvent
    private fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!isEnabled()) return
        if (!config.joinLeaveMessage.enabled) return
        if (!event.isWidget(TabWidget.PLAYER_LIST)) return

        currentLobbyPlayers.clear()

        tabPlayerName.matchAll(event.lines.map { it.string }) {
            val name = group("name")
            if (!name.equals(PlayerUtils.getName(), ignoreCase = true)) {
                currentLobbyPlayers.add(name)
            }
        }

        val playerJoined = currentLobbyPlayers.filter { it in personOfInterest && it !in notifyList }.toSet()
        val playerLeft = personOfInterest.filter { it in notifyList && it !in currentLobbyPlayers }.toSet()

        if (playerJoined.isNotEmpty()) {
            ChatUtils.chat(
                String.format(config.joinLeaveMessage.joinMessage.replace("&&", "§"), playerJoined.joinToString(", ")),
                prefix = config.joinLeaveMessage.usePrefix,
            )
            notifyList.addAll(playerJoined)
        }

        if (playerLeft.isNotEmpty()) {
            ChatUtils.chat(
                String.format(config.joinLeaveMessage.leftMessage.replace("&&", "§"), playerLeft.joinToString(", ")),
                prefix = config.joinLeaveMessage.usePrefix,
            )
            notifyList.removeAll(playerLeft)
        }
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(31, "markedPlayers", "gui.markedPlayers")
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shmarkplayer") {
            description = "Add a highlight effect to a player for better visibility"
            argCallback(
                "name",
                BrigadierArguments.string(),
                PlayerSuggestions.builder {
                    includeAllSources()
                    includePlayers(playerNamesToMark)
                    exclude(PlayerNameSource.SELF)
                },
            ) { name ->

                if (name.equals(PlayerUtils.getName(), ignoreCase = true)) {
                    ChatUtils.userError("You can't add or remove yourself this way! Go to the settings and toggle 'Mark your own name'.")
                    return@argCallback
                }

                val storedName = getStoredName(name)

                if (storedName == null) {
                    playerNamesToMark.add(name)
                    findPlayers()
                    ChatUtils.chat("§aMarked §eplayer §b$name§e!")
                } else {
                    markedPlayers[storedName]?.let {
                        RenderLivingEntityHelper.removeEntityColor(it)
                    }
                    markedPlayers.remove(storedName)
                    playerNamesToMark.remove(storedName)
                    ChatUtils.chat("§cUnmarked §eplayer §b$name§e!")
                }
            }
            simpleCallback {
                ChatUtils.userError("Usage: /shmarkplayer <name>")
            }
        }
    }
}
