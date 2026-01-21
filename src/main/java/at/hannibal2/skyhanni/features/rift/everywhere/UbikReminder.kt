package at.hannibal2.skyhanni.features.rift.everywhere

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AutoUpdatingItemStack
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting
import kotlin.time.Duration.Companion.hours

@SkyHanniModule
object UbikReminder {

    private val config get() = RiftApi.config.area.mountaintop

    private val patternGroup = RepoPattern.group("rift.ubik")

    private val cube by AutoUpdatingItemStack("UBIKS_CUBE")

    /**
     * REGEX-TEST: ROUND 7 (FINAL): You chose STEAL and gained 55,000 Motes!
     */
    private val ubikRoundPattern by patternGroup.pattern(
        "reminder-nocolor",
        "ROUND \\d+ \\(FINAL\\): You chose \\w+ and gained [\\d,]+ Motes!",
    )

    /**
     * REGEX-TEST: SPLIT! You need to wait 1h 23m 45s before you can play again.
     * REGEX-TEST: SPLIT! You need to wait 1h 23m before you can play again.
     * REGEX-TEST: SPLIT! You need to wait 12m 34s before you can play again.
     * REGEX-TEST: SPLIT! You need to wait 12m before you can play again.
     * REGEX-TEST: SPLIT! You need to wait 12s before you can play again.
     */
    private val cooldownPattern by patternGroup.pattern(
        "cooldown",
        "SPLIT! You need to wait (?<duration>.+) before you can play again\\.",
    )

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onChat(event: SkyHanniChatEvent) {
        if (!config.ubikReminder) return
        val storage = ProfileStorageData.profileSpecific?.rift ?: return
        val message = event.cleanMessage

        if (ubikRoundPattern.matches(message)) {
            storage.ubikRemindTime = 2.hours.fromNow()
            return
        }

        cooldownPattern.matchMatcher(message) {
            storage.ubikRemindTime = TimeUtils.getDuration(group("duration")).fromNow()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        val storage = ProfileStorageData.profileSpecific?.rift ?: return
        if (storage.ubikRemindTime.isInFuture()) return
        if (config.ubikReminder) {
            ChatUtils.chat(
                componentBuilder {
                    appendWithColor("Ubik's Cube is ready in the Rift!", ChatFormatting.GREEN)
                }
            )
        }
        storage.ubikRemindTime = SimpleTimeMark.farFuture()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.ubikReminder || !config.ubikGui) return
        val storage = ProfileStorageData.profileSpecific?.rift ?: return
        val remindTime = storage.ubikRemindTime
        val renderable: Renderable
        if (remindTime.isFarFuture()) {
            renderable = Renderable.horizontal {
                addItemStack(cube)
                addString("§aReady!")
            }
        } else if (config.ubikOnlyWhenReady) {
            return
        } else {
            renderable = Renderable.horizontal {
                addItemStack(cube)
                addString("§e${remindTime.timeUntil().format()}")
            }
        }

        config.timerPosition.renderRenderable(renderable, posLabel = "ubik cube")
    }
}
