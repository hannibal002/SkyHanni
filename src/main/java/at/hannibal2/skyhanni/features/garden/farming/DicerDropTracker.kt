package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
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
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object DicerDropTracker {
    private var display = emptyList<List<Any>>()
    private val itemDrops = mutableListOf<ItemDrop>()
    private val config get() = SkyHanniMod.feature.garden.dicerCounters
    private val tracker = SkyHanniTracker("Dicer Drop Tracker", { Data() }, { it.garden.dicerDropTracker }) { update() }

    class Data : TrackerData() {
        override fun reset() {
            drops.clear()
        }

        @Expose
        var drops: MutableMap<CropType, MutableMap<DropRarity, Int>> = mutableMapOf()
    }

    init {
        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.UNCOMMON, "§a§lUNCOMMON DROP! §r§eDicer dropped §r§a(\\d+)x §r§aEnchanted Melon§r§e!".toRegex()))
        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.RARE, "§9§lRARE DROP! §r§eDicer dropped §r§a(\\d+)x §r§aEnchanted Melon§r§e!".toRegex()))
        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.CRAZY_RARE, "§d§lCRAZY RARE DROP! §r§eDicer dropped §r§[a|9](\\d+)x §r§[a|9]Enchanted Melon(?: Block)?§r§e!".toRegex()))
        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.PRAY_TO_RNGESUS, "§5§lPRAY TO RNGESUS DROP! §r§eDicer dropped §r§9(\\d+)x §r§9Enchanted Melon Block§r§e!".toRegex()))

        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.UNCOMMON, "§a§lUNCOMMON DROP! §r§eDicer dropped §r§a(\\d+)x §r§aEnchanted Pumpkin§r§e!".toRegex()))
        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.RARE, "§9§lRARE DROP! §r§eDicer dropped §r§a(\\d+)x §r§aEnchanted Pumpkin§r§e!".toRegex()))
        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.CRAZY_RARE, "§d§lCRAZY RARE DROP! §r§eDicer dropped §r§a(\\d+)x §r§aEnchanted Pumpkin§r§e!".toRegex()))
        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.PRAY_TO_RNGESUS, "§5§lPRAY TO RNGESUS DROP! §r§eDicer dropped §r§[a|9](\\d+)x §r§(aEnchanted|9Polished) Pumpkin§r§e!".toRegex()))
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
            if (drop.pattern.matches(message)) {
                addDrop(drop.crop, drop.rarity)
                if (config.hideChat) {
                    event.blockedReason = "dicer_drop_tracker"
                }
                return
            }
        }
    }

    private fun update() {
        tracker.currentDisplay()?.let {
            display = drawDisplay(it)
        }
    }

    private fun drawDisplay(storage: Data) = buildList<List<Any>> {
        val cropInHand = cropInHand ?: return@buildList
        val items = storage.drops.getOrPut(cropInHand) { mutableMapOf() }
        addAsSingletonList("§7Dicer Drop Tracker for $toolName§7:")
        tracker.addDisplayModeToggle(this)
        for ((rarity, amount) in items.sortedDesc()) {
            val displayName = rarity.displayName
            addAsSingletonList(" §7- §e${amount.addSeparators()}x $displayName")
        }

        tracker.addSessionResetButton(this)
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
        tracker.modify {
            val map = it.drops.getOrPut(crop) { mutableMapOf() }
            map.addOrPut(rarity, 1)
        }
        update()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        tracker.renderDisplay(config.pos, display)
    }

    class ItemDrop(val crop: CropType, val rarity: DropRarity, val pattern: Regex)

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

    fun resetCommand(args: Array<String>) {
        tracker.resetCommand(args, "shresetdicertracker")
    }
}
