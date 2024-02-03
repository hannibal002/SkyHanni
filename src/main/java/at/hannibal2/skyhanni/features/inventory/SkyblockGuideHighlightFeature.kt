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

private const val keyPrefixInventory = "inventory"
private const val keyPrefixLoreCondition = "condition.lore"

class SkyblockGuideHighlightFeature private constructor(
    private val config: () -> Boolean,
    inventory: RepoPattern,
    loreCondition: RepoPattern,
    private val onSlotClicked: (GuiContainerEvent.SlotClickEvent) -> Unit = {},
) {

    private val inventoryPattern by inventory
    private val conditionPattern by loreCondition

    private constructor(
        config: () -> Boolean,
        key: String,
        @Language("RegExp")
        inventory: String,
        @Language("RegExp")
        loreCondition: String,
        onSlotClicked: (GuiContainerEvent.SlotClickEvent) -> Unit = {},
    ) : this(config, group.pattern("$key.$keyPrefixInventory", inventory), group.pattern("$key.$keyPrefixLoreCondition", loreCondition), onSlotClicked)

    private constructor(
        config: () -> Boolean,
        key: String,
        @Language("RegExp")
        inventory: String,
        loreCondition: RepoPattern,
        onSlotClicked: (GuiContainerEvent.SlotClickEvent) -> Unit = {},
    ) : this(config, group.pattern("$key.$keyPrefixInventory", inventory), loreCondition, onSlotClicked)

    init {
        objectList.add(this)
    }

    companion object {

        private val skyblockGuideConfig get() = SkyHanniMod.feature.inventory.skyblockGuideConfig

        fun load(skyHanniMod: SkyHanniMod) = skyHanniMod.loadModule(this)

        private val objectList = mutableListOf<SkyblockGuideHighlightFeature>()

        private var activeObject: SkyblockGuideHighlightFeature? = null
        private var missing = mutableSetOf<Int>()

        fun isEnabled() = LorenzUtils.inSkyBlock
        fun close() {
            activeObject = null
        }

        @SubscribeEvent
        fun onInventoryClose(event: InventoryCloseEvent) = close()

        @SubscribeEvent
        fun onInventoryClose(event: GuiContainerEvent.CloseWindowEvent) = close()

        @SubscribeEvent
        fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
            if (!isEnabled()) return
            val current = activeObject ?: return
            if (!missing.contains(event.slotId)) return
            current.onSlotClicked.invoke(event)
        }

        @SubscribeEvent
        fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
            if (!isEnabled()) return
            val current = activeObject ?: return

            event.gui.inventorySlots.inventorySlots
                .filter { missing.contains(it.slotNumber) }
                .forEach { it highlight LorenzColor.RED }
        }

        @SubscribeEvent
        fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
            if (!isEnabled()) return
            val current = objectList.firstOrNull { it.config.invoke() && it.inventoryPattern.matches(event.inventoryName) }
                ?: return

            missing.clear()
            activeObject = current

            for ((slot, item) in event.inventoryItems) {
                if (slot == 4) continue // Overview Item
                val lore = item.getLore()
                if (!current.conditionPattern.anyMatches(lore)) continue
                missing.add(slot)
            }
        }

        private val taskOnlyCompleteOncePattern = group.pattern("$keyPrefixLoreCondition.once", "§7§eThis task can only be completed once!")
        private val xPattern = group.pattern("$keyPrefixLoreCondition.x", "§c ?✖.*")

        init {
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.abiphoneGuide }, "abiphone", "Miscellaneous ➜ Abiphone Contac", taskOnlyCompleteOncePattern)
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.bankGuide }, "bank", "Core ➜ Bank Upgrades", taskOnlyCompleteOncePattern)
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.travelGuide }, "travel", "Core ➜ Fast Travels Unlocked", taskOnlyCompleteOncePattern)
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.spookyGuide }, "spooky", "Event ➜ Spooky Festival", taskOnlyCompleteOncePattern)
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.kuudraGuide }, "kuudra", "Slaying ➜ Defeat Kuudra", taskOnlyCompleteOncePattern)
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.beltGuide }, "belt", "Miscellaneous ➜ The Dojo", taskOnlyCompleteOncePattern)
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.jacobGuide }, "jacob", "Event ➜ Jacob's Farming Contest", taskOnlyCompleteOncePattern)
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.dragonGuide }, "dragon", "Slaying ➜ Slay Dragons", taskOnlyCompleteOncePattern)
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.storyGuide }, "story", "Story ➜ Complete Objectives", taskOnlyCompleteOncePattern)
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.rockPetGuide }, "mining.rock", "Mining ➜ Rock Milestones", taskOnlyCompleteOncePattern)
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.dolphinGuide }, "fishing.dolphin", "Fishing ➜ Dolphin Milestones", taskOnlyCompleteOncePattern)
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.essenceGuide }, "essence", "Essence Shop ➜.*", xPattern)
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.minionGuide }, "minion", "Crafted Minions", xPattern)
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.slayerDefeatGuide }, "slayer.defeat", "Slaying ➜ Defeat Slayers", xPattern)
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.harpGuide }, "harp", "Miscellaneous ➜ Harp Songs", xPattern)
            SkyblockGuideHighlightFeature({ skyblockGuideConfig.consumableGuide }, "consumable", "Miscellaneous ➜ Consumable Items", "§7§eThis task can be completed \\d+ times!")
        }
    }
}
