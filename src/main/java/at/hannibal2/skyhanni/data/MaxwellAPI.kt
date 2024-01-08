package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpaceAndResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object MaxwellAPI {
    val config = ProfileStorageData.profileSpecific
    var currentPower: MaxwellPowers? = null
    var magicalPower = -1

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
        "inventory.mp",
        "§7Magical Power: §6(?<mp>[\\d,]+)"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val message = event.message.trimWhiteSpaceAndResets().removeResets()

        chatPowerpattern.matchMatcher(message) {
            val power = group("power")
            currentPower = byNameOrNull(power) ?: return
            savePower(currentPower)
        }
    }

    @SubscribeEvent
    fun onInventoryFullyLoaded(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (event.inventoryName.contains("Accessory Bag Thaumaturgyr")) {
            val stacks = event.inventoryItems
            val selectedPower =
                stacks.values.find { it.getLore().isNotEmpty() && it.getLore().last() == "§aPower is selected!" }
                    ?: return

            currentPower = byNameOrNull(selectedPower.getLore().first()) ?: return
            savePower(currentPower)

        } else if (event.inventoryName.contains("Your Bags")) {
            val stacks = event.inventoryItems

            for (stack in stacks.values) {
                val lore = stack.getLore()
                for (line in lore) {
                    inventoryPowerPattern.matchMatcher(line) {
                        val power = group("power")
                        currentPower = byNameOrNull(power) ?: return
                        savePower(currentPower)
                    }
                    inventoryMPPattern.matchMatcher(line) {
                        // Mp is boosted in catacombs
                        if (IslandType.CATACOMBS.isInIsland()) return

                        val mp = group("mp")
                        magicalPower = mp.replace(",", "").toIntOrNull() ?: return
                        saveMP(magicalPower)
                    }
                }
            }
        }
    }

    fun byNameOrNull(name: String) = MaxwellPowers.entries.find { it.power == name }

    // Handle storage
    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val config = ProfileStorageData.profileSpecific ?: return
        currentPower = config.maxwell.currentPower
        magicalPower = config.maxwell.magicalPower
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        val config = ProfileStorageData.profileSpecific ?: return
        currentPower = config.maxwell.currentPower ?: null
        magicalPower = config.maxwell.magicalPower
    }

    private fun savePower(power: MaxwellPowers?) {
        if (power == null) return
        val config = ProfileStorageData.profileSpecific ?: return
        config.maxwell.currentPower = power
    }

    private fun saveMP(mp: Int?) {
        if (mp == null) return
        val config = ProfileStorageData.profileSpecific ?: return
        config.maxwell.magicalPower = mp
    }
}
