package at.hannibal2.skyhanni.features.rift.area.dreadfarm

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.jsonobjects.repo.RiftWoodenButtonsJson
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.features.rift.RiftAPI.isBlowgun
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

// make sure hit buttons counts even when you aren't tracking the Buttons soul
// this is required in case someone hits buttons and then later starts tracking souls (within same server)

// onWorldChange empty hitButtons

@SkyHanniModule
object WoodenButtonsHelper {

    private val patternGroup = RepoPattern.group("rift.area.dreadfarm.buttons")
    /**
     * REGEX-TEST: §eYou have hit §r§b1/56 §r§eof the wooden buttons!
     * REGEX-TEST: §eYou have hit §r§b10/56 §r§eof the wooden buttons!
     */
    private val buttonHitPattern by patternGroup.pattern(
        "hit",
        "§eYou have hit §r§b\\d+/56 §r§eof the wooden buttons!"
    )

    private var buttonLocations = mapOf<String, LorenzVec>()
    private var hitButtons = mutableSetOf<String>()
    private var lastHitButton: String? = null

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<RiftWoodenButtonsJson>("RiftWoodenButtons")
        val houses = data.houses
        var index = 0
        buttonLocations = buildMap {
            for ((_, house) in houses) {
                for (location in house) {
                    this[index++.toString()] = location
                }
            }
        }
    }

    @SubscribeEvent
    fun onItemClick(event: ItemClickEvent) {
        if (!RiftAPI.inRift()) return
        if (event.clickType != ClickType.RIGHT_CLICK) return
        if (event.itemInHand?.isBlowgun != true) return
        ChatUtils.debug("Berberis blowgun fired with right click!")
    }

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!RiftAPI.inRift()) return

        val location = event.position
        if (location.getBlockAt() !== Blocks.wooden_button) return

        val hitButton = buttonLocations.entries.find { it.value == location }?.key ?: return
        lastHitButton = hitButton
        ChatUtils.debug("New hit button: $lastHitButton")
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!RiftAPI.inRift()) return
        buttonHitPattern.matchMatcher(event.message) {
            lastHitButton?.let {
                if (!hitButtons.contains(it)) {
                    hitButtons.add(it)
                    ChatUtils.debug("Added $lastHitButton to hit buttons.")
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        hitButtons.clear()
    }
}
