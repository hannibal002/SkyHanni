package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DicerRngDropCounter {
    private var display = listOf<String>()
    private val drops = mutableMapOf<CropType, MutableMap<DropRarity, Int>>()
    private val itemDrops = mutableListOf<ItemDrop>()

    init {
        drops[CropType.MELON] = mutableMapOf()
        drops[CropType.PUMPKIN] = mutableMapOf()

        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.UNCOMMON, "§a§lUNCOMMON DROP! §r§eDicer dropped §r§a1x §r§aEnchanted Melon§r§e!"))
        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.RARE, "§9§lRARE DROP! §r§eDicer dropped §r§a5x §r§aEnchanted Melon§r§e!"))
        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.CRAZY_RARE, "§d§lCRAZY RARE DROP! §r§eDicer dropped §r§a50x §r§aEnchanted Melon§r§e!"))
        itemDrops.add(ItemDrop(CropType.MELON, DropRarity.PRAY_TO_RNGESUS, "§5§lPRAY TO RNGESUS DROP! §r§eDicer dropped §r§92x §r§9Enchanted Melon Block§r§e!"))

        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.UNCOMMON, "§a§lUNCOMMON DROP! §r§eDicer dropped §r§f64x §r§fPumpkin§r§e!"))
        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.RARE, "§9§lRARE DROP! §r§eDicer dropped §r§a1x §r§aEnchanted Pumpkin§r§e!"))
        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.CRAZY_RARE, "§d§lCRAZY RARE DROP! §r§eDicer dropped §r§a10x §r§aEnchanted Pumpkin§r§e!"))
        itemDrops.add(ItemDrop(CropType.PUMPKIN, DropRarity.PRAY_TO_RNGESUS, "§5§lPRAY TO RNGESUS DROP! §r§eDicer dropped §r§a64x §r§aEnchanted Pumpkin§r§e!"))
    }

    enum class DropRarity(val displayName: String) {
        UNCOMMON("§a§lUNCOMMON DROP"),
        RARE("§9§lRARE DROP"),
        CRAZY_RARE("§d§lCRAZY RARE DROP"),
        PRAY_TO_RNGESUS("§5§lPRAY TO RNGESUS DROP"),
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        val message = event.message
        for (drop in itemDrops) {
            if (message == drop.message) {
                addDrop(drop.crop, drop.rarity)
                saveConfig()
                update()
                return
            }
        }

        if (message.contains("§r§eDicer dropped")) {
            LorenzUtils.debug("Unknown dicer drop message!")
            println("Unknown dicer drop message: '$message'")
        }
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay(): List<String> {
        val help = mutableListOf<String>()
        val items = drops[cropInHand] ?: return help
        help.add("§7RNG Drops for $toolName§7:")
        for ((rarity, amount) in items.sortedDesc()) {
            val displayName = rarity.displayName
            help.add(" §7- §e${amount}x $displayName")
        }

        return help
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
        val map = drops[crop]!!
        val old = map[rarity] ?: 0
        map[rarity] = old + 1
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (isEnabled()) {
            SkyHanniMod.feature.garden.dicerCounterPos.renderStrings(display, posLabel = "Dicer Counter")
        }
    }

    class ItemDrop(val crop: CropType, val rarity: DropRarity, val message: String)

    private fun saveConfig() {
        val list = SkyHanniMod.feature.hidden.gardenDicerRngDrops
        list.clear()
        for (drop in drops) {
            val crop = drop.key
            for ((rarity, amount) in drop.value) {
                list[crop.cropName + "." + rarity.name] = amount
            }
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        for ((internalName, amount) in SkyHanniMod.feature.hidden.gardenDicerRngDrops) {
            val split = internalName.split(".")
            val crop = CropType.getByNameNoNull(split[0])
            val rarityName = split[1]
            val rarity = DropRarity.valueOf(rarityName)
            drops[crop]!![rarity] = amount
        }
    }

    fun isEnabled() = GardenAPI.inGarden() && SkyHanniMod.feature.garden.dicerCounterDisplay
}