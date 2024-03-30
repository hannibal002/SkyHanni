package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.config.features.garden.pests.StereoHarmonyConfig
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
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

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        val vinyl = activeVinyl ?: return@buildList
        val pest = vinyl.getPest()
        val displayType = config.displayType.get()

        if ((displayType == StereoHarmonyConfig.DisplayType.HEAD ||
            displayType == StereoHarmonyConfig.DisplayType.BOTH) && pest != null) {
            val itemStack = pest.internalName.getItemStack()
            add(Renderable.itemStack(itemStack, 1.68))
        }
        val vinylName = vinyl.displayName
        add(Renderable.string("§ePlaying: §a$vinylName"))
        if ((displayType == StereoHarmonyConfig.DisplayType.NAME ||
            displayType == StereoHarmonyConfig.DisplayType.BOTH) && pest != null) {
            val pestName = pest.displayName
            add(Renderable.string(" §e(§c$pestName§e)"))
        }
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
        if (activeVinyl == VinylType.NONE && config.hideWhenNone.get()) return
        else if (display.isEmpty()) update()
        config.position.renderStringsAndItems(listOf(display), posLabel = "Stereo Harmony Display")
    }

    @SubscribeEvent
    fun onWorldSwitch(event: LorenzWorldChangeEvent) {
        display = emptyList()
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(
            config.displayEnabled,
            config.displayType,
            config.hideWhenNone
        ) {
            update()
        }
    }

    fun isEnabled() = GardenAPI.inGarden() && config.displayEnabled.get()
}
