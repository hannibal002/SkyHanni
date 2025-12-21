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
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.hours

@SkyHanniModule
object UbikReminder {

    private val config get() = RiftApi.config.area.mountaintop

    private val patternGroup = RepoPattern.group("rift.ubik")

    private val cube by AutoUpdatingItemStack("UBIKS_CUBE")

    /**
     * REGEX-TEST: §6§lROUND 7 §r§6(§r§lFINAL§r§6)§r§l: §r§eYou chose §r§c§lSTEAL §r§eand gained §r§55,000 Motes§r§e!
     * REGEX-TEST: §6§lROUND 7 §r§6(§r§6§lFINAL§r§6)§r§6§l: §r§eYou chose §r§c§lSTEAL §r§eand gained §r§55,000 Motes§r§e!
     */
    private val ubikRoundPattern by patternGroup.pattern(
        "reminder",
        "§6§lROUND [5-9] §r§6\\(§r(?:§6)?§lFINAL§r§6\\)§r(?:§6)?§l: §r§eYou chose .*",
    )

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onChat(event: SkyHanniChatEvent) {
        if (!config.ubikReminder) return
        val storage = ProfileStorageData.profileSpecific?.rift ?: return
        if (ubikRoundPattern.matches(event.message)) {
            storage.ubikRemindTime = 2.hours.fromNow()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        val storage = ProfileStorageData.profileSpecific?.rift ?: return
        if (storage.ubikRemindTime.isInFuture()) return
        if (config.ubikReminder) {
            ChatUtils.chat("§aUbik's Cube is ready in the Rift!")
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
