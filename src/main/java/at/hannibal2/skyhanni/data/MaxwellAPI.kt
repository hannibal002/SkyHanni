package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.jsonobjects.repo.MaxwellPowersJson
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object MaxwellAPI {

    private val storage get() = ProfileStorageData.profileSpecific

    var currentPower: String?
        get() = storage?.maxwell?.currentPower
        set(value) {
            storage?.maxwell?.currentPower = value ?: return
        }
    var magicalPower: Int?
        get() = storage?.maxwell?.magicalPower
        set(value) {
            storage?.maxwell?.magicalPower = value ?: return
        }

    private var powers = mutableListOf<String>()

    private val group = RepoPattern.group("data.maxwell")
    private val chatPowerpattern by group.pattern(
        "chat.power",
        "§eYou selected the §a(?<power>.*) §e(power )?for your §aAccessory Bag§e!"
    )
    private val inventoryPowerPattern by group.pattern(
        "inventory.power",
        "§7Selected Power: §a(?<power>.*)"
    )
    private val inventoryMPPattern by group.pattern(
        "inventory.magicalpower",
        "§7Magical Power: §6(?<mp>[\\d,]+)"
    )
    private val thaumaturgyGuiPattern by group.pattern(
        "gui.thaumaturgy",
        "Accessory Bag Thaumaturgy"
    )
    private val yourBagsGuiPattern by group.pattern(
        "gui.yourbags",
        "Your Bags"
    )
    private val powerSelectedPattern by group.pattern(
        "gui.selectedpower",
        "§aPower is selected!"
    )
    private val accessoryBagStack by group.pattern(
        "stack.accessorybag",
        "§.Accessory Bag"
    )
    private val redstoneCollectionRequirementPattern by group.pattern(
        "collection.redstone.requirement",
        "(?:§.)*Requires (?:§.)*Redstone Collection I+(?:§.)*\\."
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val message = event.message.trimWhiteSpace().removeResets()

        chatPowerpattern.matchMatcher(message) {
            val power = group("power")
            currentPower = getPowerByNameOrNull(power)
                ?: return ErrorManager.logErrorWithData(
                    UnknownMaxwellPower("Unknown power: $power"),
                    "Unknown power: $power",
                    "power" to power,
                    "message" to message
                )
        }
    }

    @SubscribeEvent
    fun onInventoryFullyLoaded(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return

        if (thaumaturgyGuiPattern.matches(event.inventoryName)) {
            val selectedPowerStack =
                event.inventoryItems.values.find {
                    powerSelectedPattern.matches(it.getLore().lastOrNull())
                } ?: return
            val displayName = selectedPowerStack.displayName.removeColor().trim()

            currentPower = getPowerByNameOrNull(displayName)
                ?: return ErrorManager.logErrorWithData(
                    UnknownMaxwellPower("Unknown power: $displayName"),
                    "Unknown power: $displayName",
                    "displayName" to displayName,
                    "lore" to selectedPowerStack.getLore(),
                    noStackTrace = true
                )
            return
        }

        if (yourBagsGuiPattern.matches(event.inventoryName)) {
            val stacks = event.inventoryItems

            for (stack in stacks.values) {
                if (accessoryBagStack.matches(stack.displayName)) processStack(stack)
            }
        }
    }

    private fun processStack(stack: ItemStack) {
        for (line in stack.getLore()) {
            redstoneCollectionRequirementPattern.matchMatcher(line) {
                // Redstone Collection is required for the bag
                currentPower = "Redstone Collection"
                return@matchMatcher
            }

            inventoryMPPattern.matchMatcher(line) {
                // MagicalPower is boosted in catacombs
                if (IslandType.CATACOMBS.isInIsland()) return@matchMatcher

                val mp = group("mp")
                magicalPower = mp.formatInt()
                return@matchMatcher
            }

            inventoryPowerPattern.matchMatcher(line) {
                val power = group("power")
                currentPower = getPowerByNameOrNull(power)
                    ?: return@matchMatcher ErrorManager.logErrorWithData(
                        UnknownMaxwellPower("Unknown power: ${stack.displayName}"),
                        "Unknown power: ${stack.displayName}",
                        "displayName" to stack.displayName,
                        "lore" to stack.getLore(),
                        noStackTrace = true
                    )
                return@matchMatcher
            }
        }
    }

    private fun getPowerByNameOrNull(name: String) = powers.find { it == name }

    fun isEnabled() = LorenzUtils.inSkyBlock && storage != null

    // Load powers from repo
    @SubscribeEvent
    fun onRepoLoad(event: RepositoryReloadEvent) {
        val data = event.getConstant<MaxwellPowersJson>("MaxwellPowers")
        powers = data.powers
    }

    class UnknownMaxwellPower(message: String) : Exception(message)
}
