package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class StereoHarmonyDisplay {

    private val config get() = PestAPI.config.stereoHarmony

    private var activeVinyl: VinylType?
        get() = ProfileStorageData.profileSpecific?.garden?.activeVinyl
        private set(type) {
            ProfileStorageData.profileSpecific?.garden?.activeVinyl = type
        }

    private fun VinylType.getPest() = PestType.entries.find { it.vinyl == this }

    private val vinylTypeGroup = RepoPattern.group("garden.vinyl")

    /**
     * REGEX-TEST: §aYou are now playing §r§eNot Just a Pest§r§a!
     */
    private val selectVinylPattern by vinylTypeGroup.pattern(
        "select",
        "§aYou are now playing §r§e(?<type>.*)§r§a!"
    )

    /**
     * REGEX-TEST: §aYou are no longer playing §r§eNot Just a Pest§r§a!
     */
    private val unselectVinylPattern by vinylTypeGroup.pattern(
        "unselect",
        "§aYou are no longer playing §r§e.*§r§a!"
    )

    private var display = emptyList<Renderable>()

    private val questionMarkSkull = ItemUtils.createSkull(
        "§c?",
        "28aa984a-2077-40cc-8de7-e641adf2c497",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDZiY" +
            "TYzMzQ0ZjQ5ZGQxYzRmNTQ4OGU5MjZiZjNkOWUyYjI5OTE2YTZjNTBkNjEwYmI0MGE1MjczZGM4YzgyIn19fQ=="
    )

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        val vinyl = activeVinyl ?: return@buildList
        val pest = vinyl.getPest()

        val itemStack = pest?.internalName?.getItemStack() ?: questionMarkSkull
        if (config.showHead.get()) add(Renderable.itemStack(itemStack, 1.67))
        val list = mutableListOf<Renderable>()
        val vinylName = vinyl.displayName
        val pestName = pest?.displayName ?: "None"
        list.add(Renderable.string("§ePlaying: §a$vinylName"))
        val pestLine = mutableListOf<Renderable>()
        pestLine.add(Renderable.string("§ePest: §c$pestName "))
        if (pest?.crop != null && config.showCrop.get()) pestLine.add(Renderable.itemStack(pest.crop.icon))
        list.add(Renderable.horizontalContainer(pestLine))
        add(Renderable.verticalContainer(list, verticalAlign = RenderUtils.VerticalAlignment.CENTER))
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!GardenAPI.inGarden()) return
        selectVinylPattern.matchMatcher(event.message) {
            activeVinyl = VinylType.getByName(group("type"))
            update()
        }
        if (unselectVinylPattern.matches(event.message)) {
            activeVinyl = VinylType.NONE
            update()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!GardenAPI.isCurrentlyFarming() && !config.alwaysShow) return

        if (activeVinyl == VinylType.NONE && config.hideWhenNone) return
        else if (display.isEmpty()) update()
        if (display.isEmpty()) return
        val content = Renderable.horizontalContainer(display, 1, verticalAlign = RenderUtils.VerticalAlignment.CENTER)
        val renderables = listOf(content)
        config.position.renderRenderables(renderables, posLabel = "Stereo Harmony Display")
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        display = emptyList()
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.showHead, config.showCrop) { update() }
    }

    fun isEnabled() = GardenAPI.inGarden() && config.displayEnabled
}
