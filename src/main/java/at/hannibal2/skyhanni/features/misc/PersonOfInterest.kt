package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object PersonOfInterest {

    private val config get() = SkyHanniMod.feature.misc.personOfInterest
    private val patternGroup = RepoPattern.group("misc.personofinterest")

    /**
     * REGEX-TEST: §8[§r§6400§r§8] §r§6HiZe_ §r§6▒
     * REGEX-TEST: §8[§r§9318§r§8] §r§bwings_wacr §r§b§lᛝ
     * REGEX-TEST: §8[§r§d321§r§8] §r§bbotbob21 §r§b§lᛝ
     * REGEX-TEST: §8[§r§f42§r§8] §r§aVoidW_
     * REGEX-TEST: §8[§r§a151§r§8] §r§bPhoenix_325
     */
    private val player by patternGroup.pattern(
        "player",
        "§8\\[§r(?<level>.*)§r§8] §r§\\w(?<name>[A-z0-9_]+)(?<symbol>.*)?",
    )

    private val notifyList = mutableSetOf<String>()
    private val currentLobbyPlayers = mutableSetOf<String>()
    private var personOfInterest = listOf<String>()

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (!isEnabled()) return
        notifyList.clear()
        currentLobbyPlayers.clear()
    }

    @SubscribeEvent
    fun onTablistUpdate(event: WidgetUpdateEvent) {
        if (!isEnabled()) return
        if (!event.isWidget(TabWidget.PLAYER_LIST)) return

        currentLobbyPlayers.clear()

        loop@ for (line in event.lines) {
            player.matchMatcher(line) {
                val name = group("name")
                if (name == LorenzUtils.getPlayerName()) continue@loop
                currentLobbyPlayers.add(name)
            }
        }

        val playerJoined = currentLobbyPlayers.filter { it in personOfInterest && it !in notifyList }
        val playerLeft = personOfInterest.filter { it in notifyList && it !in currentLobbyPlayers }

        if (playerJoined.isNotEmpty()) {
            ChatUtils.chat(
                String.format(config.joinMessage.replace("&&", "§"), playerJoined.joinToString(", ")),
                config.usePrefix,
            )
            notifyList.addAll(playerJoined)
        }

        if (playerLeft.isNotEmpty()) {
            ChatUtils.chat(
                String.format(config.leftMessage.replace("&&", "§"), playerLeft.joinToString(", ")),
                config.usePrefix,
            )
            notifyList.removeAll(playerLeft)
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.playersList) {
            personOfInterest = config.playersList.get().split(",").map { it.trim() }
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
