package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.anyMatches
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.intellij.lang.annotations.Language

val group = RepoPattern.group("skyblockguide.highlight")

class SkyblockGuideHighlightFeature private constructor(
    key: String,
    private val config: () -> Boolean,
    @Language("RegExp")
    inventory: String,
    @Language("RegExp")
    loreCondition: String,
    private val onSlotClicked: (GuiContainerEvent.SlotClickEvent) -> Unit = {},
) {

    private val inventoryName by group.pattern("$key.inventory", inventory)
    private val condition by group.pattern("$key.condition.lore", loreCondition)

    private var missing = mutableSetOf<Int>()
    private var inInventory = false

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (!inventoryName.matches(event.inventoryName)) return

        inInventory = true

        missing.clear()

        for ((slot, item) in event.inventoryItems) {
            if (slot == 4) continue // Overview Item
            val lore = item.getLore()
            if (!condition.anyMatches(lore)) continue
            missing.add(slot)
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onInventoryClose(event: GuiContainerEvent.CloseWindowEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        event.gui.inventorySlots.inventorySlots
            .filter { missing.contains(it.slotNumber) }
            .forEach { it highlight LorenzColor.RED }
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        if (!missing.contains(event.slotId)) return
        onSlotClicked.invoke(event)
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.invoke()

    init {
        objectList.add(this)
    }

    companion object {

        private val skyblockGuideConfig get() = SkyHanniMod.feature.inventory.skyblockGuideConfig

        fun load(skyHanniMod: SkyHanniMod) = objectList.forEach { skyHanniMod.loadModule(it) }

        private val objectList = mutableListOf<SkyblockGuideHighlightFeature>()

        init {
            SkyblockGuideHighlightFeature("abiphone", { skyblockGuideConfig.abiphoneGuide }, "Miscellaneous ➜ Abiphone Contac", "§7§eThis task can only be completed once!")
            SkyblockGuideHighlightFeature("bank", { skyblockGuideConfig.bankGuide }, "Core ➜ Bank Upgrades", "§7§eThis task can only be completed once!")
            SkyblockGuideHighlightFeature("travel", { skyblockGuideConfig.travelGuide }, "Core ➜ Fast Travels Unlocked", "§7§eThis task can only be completed once!")
            SkyblockGuideHighlightFeature("spooky", { skyblockGuideConfig.spookyGuide }, "Event ➜ Spooky Festival", "§7§eThis task can only be completed once!")
            SkyblockGuideHighlightFeature("kuudra", { skyblockGuideConfig.kuudraGuide }, "Slaying ➜ Defeat Kuudra", "§7§eThis task can only be completed once!")
            SkyblockGuideHighlightFeature("belt", { skyblockGuideConfig.beltGuide }, "Miscellaneous ➜ The Dojo", "§7§eThis task can only be completed once!")
            SkyblockGuideHighlightFeature("jacob", { skyblockGuideConfig.jacobGuide }, "Event ➜ Jacob's Farming Contest", "§7§eThis task can only be completed once!")
            SkyblockGuideHighlightFeature("dragon", { skyblockGuideConfig.dragonGuide }, "Slaying ➜ Slay Dragons", "§7§eThis task can only be completed once!")
            SkyblockGuideHighlightFeature("story", { skyblockGuideConfig.storyGuide }, "Story ➜ Complete Objectives", "§7§eThis task can only be completed once!")
            SkyblockGuideHighlightFeature("mining.rock", { skyblockGuideConfig.rockPetGuide }, "Mining ➜ Rock Milestones", "§7§eThis task can only be completed once!")
            SkyblockGuideHighlightFeature("fishing.dolphin", { skyblockGuideConfig.dolphinGuide }, "Fishing ➜ Dolphin Milestones", "§7§eThis task can only be completed once!")
            SkyblockGuideHighlightFeature("essence", { skyblockGuideConfig.essenceGuide }, "Essence Shop ➜.*", "§c ?✖.*")
            SkyblockGuideHighlightFeature("minion", { skyblockGuideConfig.minionGuide }, "Crafted Minions", "§c✖.*")
            SkyblockGuideHighlightFeature("slayer.defeat", { skyblockGuideConfig.slayerDefeatGuide }, "Slaying ➜ Defeat Slayers", "§c ✖.*")
            SkyblockGuideHighlightFeature("harp", { skyblockGuideConfig.harpGuide }, "Miscellaneous ➜ Harp Songs", "§c ✖.*")
            SkyblockGuideHighlightFeature("consumable", { skyblockGuideConfig.consumableGuide }, "Miscellaneous ➜ Consumable Items", "§7§eThis task can be completed \\d+ times!")
        }
    }
}
