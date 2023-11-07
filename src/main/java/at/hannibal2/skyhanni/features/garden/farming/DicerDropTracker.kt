package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.Storage.ProfileSpecific.GardenStorage.DicerDropTracker
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addOrPut
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.tracker.DisplayMode
import at.hannibal2.skyhanni.utils.tracker.SharedTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerUtils
import at.hannibal2.skyhanni.utils.tracker.TrackerUtils.addDisplayModeToggle
import at.hannibal2.skyhanni.utils.tracker.TrackerUtils.addSessionResetButton
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

object DicerDropTracker {
    private var display = emptyList<List<Any>>()
    private val itemDrops = mutableListOf<ItemDrop>()
    private val config get() = SkyHanniMod.feature.garden.dicerCounters
    private val currentSessionData = DicerDropTracker()
    private var inventoryOpen = false

    // TODO USE SH-REPO
    private val melonUncommonDropPattern =
        "§a§lUNCOMMON DROP! §r§eDicer dropped §r§a(\\d+)x §r§aEnchanted Melon§r§e!".toPattern()
    private val melonRareDropPattern =
        "§9§lRARE DROP! §r§eDicer dropped §r§a(\\d+)x §r§aEnchanted Melon§r§e!".toPattern()
    private val melonCrazyRareDropPattern =
        "§d§lCRAZY RARE DROP! §r§eDicer dropped §r§[a|9](\\d+)x §r§[a|9]Enchanted Melon(?: Block)?§r§e!".toPattern()
    private val melonRngesusDropPattern =
        "§5§lPRAY TO RNGESUS DROP! §r§eDicer dropped §r§9(\\d+)x §r§9Enchanted Melon Block§r§e!".toPattern()

    private val pumpkinUncommonDropPattern =
        "§a§lUNCOMMON DROP! §r§eDicer dropped §r§a(\\d+)x §r§aEnchanted Pumpkin§r§e!".toPattern()
    private val pumpkinRareDropPattern =
        "§9§lRARE DROP! §r§eDicer dropped §r§a(\\d+)x §r§aEnchanted Pumpkin§r§e!".toPattern()
    private val pumpkinCrazyRareDropPattern =
        "§d§lCRAZY RARE DROP! §r§eDicer dropped §r§a(\\d+)x §r§aEnchanted Pumpkin§r§e!".toPattern()
    private val pumpkinRngesusDropPattern =
        "§5§lPRAY TO RNGESUS DROP! §r§eDicer dropped §r§[a|9](\\d+)x §r§(aEnchanted|9Polished) Pumpkin§r§e!".toPattern()

    init {
        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.UNCOMMON, melonUncommonDropPattern))
        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.RARE, melonRareDropPattern))
        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.CRAZY_RARE, melonCrazyRareDropPattern))
        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.PRAY_TO_RNGESUS, melonRngesusDropPattern))

        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.UNCOMMON, pumpkinUncommonDropPattern))
        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.RARE, pumpkinRareDropPattern))
        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.CRAZY_RARE, pumpkinCrazyRareDropPattern))
        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.PRAY_TO_RNGESUS, pumpkinRngesusDropPattern))
    }

    enum class DropRarity(val displayName: String) {
        UNCOMMON("§a§lUNCOMMON DROP"),
        RARE("§9§lRARE DROP"),
        CRAZY_RARE("§d§lCRAZY RARE DROP"),
        PRAY_TO_RNGESUS("§5§lPRAY TO RNGESUS DROP"),
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!GardenAPI.inGarden()) return
        if (!config.hideChat && !config.display) return

        val message = event.message
        for (drop in itemDrops) {
            drop.pattern.matchMatcher(message) {
                addDrop(drop.crop, drop.rarity)
                if (config.hideChat) {
                    event.blockedReason = "dicer_drop_tracker"
                }
                return
            }
        }
    }

    private fun update() {
        currentDisplay()?.let {
            display = drawDisplay(it)
        }
    }

    private fun drawDisplay(storage: DicerDropTracker) = buildList<List<Any>> {
        val cropInHand = cropInHand ?: return@buildList
        val items = storage.drops.getOrPut(cropInHand) { mutableMapOf() }
        addAsSingletonList("§7Dicer Drop Tracker for $toolName§7:")
        if (inventoryOpen) {
            addDisplayModeToggle {
                update()
            }
        }
        for ((rarity, amount) in items.sortedDesc()) {
            val displayName = rarity.displayName
            addAsSingletonList(" §7- §e${amount.addSeparators()}x $displayName")
        }

        if (inventoryOpen && TrackerUtils.currentDisplayMode == DisplayMode.CURRENT) {
            addSessionResetButton("Dicer Drop Tracker", getSharedTracker()) {
                update()
            }
        }
    }

    private var cropInHand: CropType? = null
    private var toolName = ""

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        val crop = event.crop
        cropInHand = if (crop == CropType.MELON || crop == CropType.PUMPKIN) crop else null
        if (cropInHand != null) {
            toolName = event.toolItem!!.name!!
        }
        update()
    }

    private fun addDrop(crop: CropType, rarity: DropRarity) {
        val sharedTracker = getSharedTracker() ?: return
        sharedTracker.modify {
            val map = it.drops.getOrPut(crop) { mutableMapOf() }
            map.addOrPut(rarity, 1)
        }
        update()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        val currentlyOpen = Minecraft.getMinecraft().currentScreen is GuiInventory
        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            update()
        }
        config.pos.renderStringsAndItems(display, posLabel = "Dicer Drop Tracker")
    }

    class ItemDrop(val crop: CropType, val rarity: DropRarity, val pattern: Pattern)

    fun isEnabled() = GardenAPI.inGarden() && config.display

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.dicerCounterDisplay", "garden.dicerCounters.display")
        event.move(3, "garden.dicerCounterHideChat", "garden.dicerCounters.hideChat")
        event.move(3, "garden.dicerCounterPos", "garden.dicerCounters.pos")

        event.move(7, "#profile.garden.dicerRngDrops", "#profile.garden.dicerDropTracker.drops") { old ->
            val items: MutableMap<CropType, MutableMap<DropRarity, Int>> = mutableMapOf()
            val oldItems = ConfigManager.gson.fromJson<Map<String, Int>>(old, Map::class.java)
            for ((internalName, amount) in oldItems) {
                val split = internalName.split(".")
                val crop = CropType.getByName(split[0])
                val rarityName = split[1]
                val rarity = DropRarity.valueOf(rarityName)
                items.getOrPut(crop) { mutableMapOf() }[rarity] = amount
            }

            ConfigManager.gson.toJsonTree(items)
        }
    }

    private fun currentDisplay() = getSharedTracker()?.getCurrent()

    private fun getSharedTracker(): SharedTracker<DicerDropTracker>? {
        val profileSpecific = ProfileStorageData.profileSpecific ?: return null
        return SharedTracker(profileSpecific.garden.dicerDropTracker, currentSessionData)
    }

    fun resetCommand(args: Array<String>) {
        TrackerUtils.resetCommand("Dicer Drop Tracker", "shresetdicertracker", args, getSharedTracker()) {
            update()
        }
    }
}
