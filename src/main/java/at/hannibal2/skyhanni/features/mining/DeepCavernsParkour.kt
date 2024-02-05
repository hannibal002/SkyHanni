package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.ParkourJson
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.toChromaColor
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.ParkourHelper
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DeepCavernsParkour {
    private val config get() = SkyHanniMod.feature.mining.deepCavernsParkour

    private var parkourHelper: ParkourHelper? = null
    private var show = false
    private var showStartIcon = false

    private val startIcon by lazy {
        val neuItem = "MAP".asInternalName().getItemStack()
        Utils.createItemStack(
            neuItem.item,
            "§bDeep Caverns Parkour",
            "§8(From SkyHanni)",
            "",
            "§7Manually enable the ",
            "§7Parkour to the bottom",
            "§7of Deep Caverns."
        )
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        parkourHelper?.reset()
        show = false
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ParkourJson>("DeepCavernsParkour")
        parkourHelper = ParkourHelper(
            data.locations,
            data.shortCuts,
            platformSize = 1.0,
            detectionRange = 3.5,
            depth = false,
            onEndReach = {
                show = false
            }
        )
        updateConfig()
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        LorenzUtils.onToggle(config.rainbowColor, config.monochromeColor, config.lookAhead) {
            updateConfig()
        }
    }

    private fun updateConfig() {
        parkourHelper?.run {
            rainbowColor = config.rainbowColor.get()
            monochromeColor = config.monochromeColor.get().toChromaColor()
            lookAhead = config.lookAhead.get() + 1
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        showStartIcon = false
        if (!isEnabled()) return
        if (event.inventoryName != "Lift") return
        if (LorenzUtils.skyBlockArea != "Gunpowder Mines") return
        showStartIcon = true

        event.inventoryItems[30]?.let {
            if (it.displayName != "§aObsidian Sanctuary") {
                if (!show) {
                    start()
                    LorenzUtils.chat("Automatically enabling Deep Caverns Parkour, helping you find the way to the bottom of Deep Caverns and the path to Ryst.")
                }
            }
        }
    }

    private fun start() {
        show = true
        parkourHelper?.reset()
        if (parkourHelper == null) {
            LorenzUtils.clickableChat(
                "DeepCavernsParkour missing in SkyHanni Repo! Try /shupdaterepo to fix it!",
                "shupdaterepo",
                prefixColor = "§c"
            )
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        showStartIcon = false
    }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (show) return
        if (event.inventory is ContainerLocalMenu && showStartIcon && event.slotNumber == 40) {
            event.replaceWith(startIcon)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onStackClick(event: SlotClickEvent) {
        if (showStartIcon && event.slotId == 40) {
            event.isCanceled = true
            LorenzUtils.chat("Manually enabled Deep Caverns Parkour.")
            start()
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (!show) return

        parkourHelper?.render(event)
    }

    fun isEnabled() = IslandType.DEEP_CAVERNS.isInIsland() && config.enabled
}
