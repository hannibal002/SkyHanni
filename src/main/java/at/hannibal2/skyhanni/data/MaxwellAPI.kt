package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.jsonobjects.repo.MaxwellPowersJson
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
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
    private val thaumatorgyrGuiPattern by group.pattern(
        "gui.thaumatorgyr",
        "Accessory Bag Thaumaturgyr"
    )
    private val yourBagsGuiPattern by group.pattern(
        "gui.yourbags",
        "Your Bags"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val message = event.message.trimWhiteSpace().removeResets()

        chatPowerpattern.matchMatcher(message) {
            val power = group("power")
            currentPower = getPowerByNameOrNull(power) ?: return
            return
        }
    }

    @SubscribeEvent
    fun onInventoryFullyLoaded(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return

        if (thaumatorgyrGuiPattern.matches(event.inventoryName)) {
            val stacks = event.inventoryItems
            val selectedPower =
                stacks.values.find {
                    val lore = it.getLore()
                    lore.isNotEmpty() && lore.last() == "§aPower is selected!"
                } ?: return

            currentPower = getPowerByNameOrNull(selectedPower.displayName) ?: return
            return
        }

        if (yourBagsGuiPattern.matches(event.inventoryName)) {
            val stacks = event.inventoryItems

            stack@for (stack in stacks.values) {
                val lore = stack.getLore()
                for (line in lore) {
                    inventoryPowerPattern.matchMatcher(line) {
                        val power = group("power")
                        currentPower = getPowerByNameOrNull(power) ?: return
                        continue@stack
                    }
                    inventoryMPPattern.matchMatcher(line) {
                        // MagicalPower is boosted in catacombs
                        if (IslandType.CATACOMBS.isInIsland()) return

                        val mp = group("mp")
                        magicalPower = mp.replace(",", "").toIntOrNull() ?: return
                        continue@stack
                    }
                }
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
}
